import base64
import io
import logging
import os
from contextlib import asynccontextmanager
from pathlib import Path
from typing import List, Optional

import requests
import torch
import uvicorn
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from PIL import Image
from transformers import AutoProcessor, SiglipModel


logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("siglip-service")


MODEL_NAME = os.getenv("SIGLIP_MODEL_NAME", "google/siglip-base-patch16-224")
MODEL_DIR = Path(os.getenv("SIGLIP_MODEL_DIR", Path(__file__).resolve().parent / "model"))
HOST = os.getenv("SIGLIP_HOST", "0.0.0.0")
PORT = int(os.getenv("SIGLIP_PORT", "8000"))
REQUEST_TIMEOUT_SECONDS = int(os.getenv("SIGLIP_REQUEST_TIMEOUT_SECONDS", "15"))


class EmbeddingRequest(BaseModel):
    imageUrl: Optional[str] = Field(default=None, description="Image URL, local path, file:// path, or data URL")
    text: Optional[str] = Field(default=None, description="Text to embed")


class EmbeddingResponse(BaseModel):
    vector: List[float]


class HealthResponse(BaseModel):
    status: str
    model: str
    device: str


class SiglipService:
    def __init__(self) -> None:
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.model: Optional[SiglipModel] = None
        self.processor = None

    def load(self) -> None:
        if self.model is not None and self.processor is not None:
            return

        MODEL_DIR.mkdir(parents=True, exist_ok=True)
        model_source = str(MODEL_DIR) if any(MODEL_DIR.iterdir()) else MODEL_NAME
        logger.info("Loading SigLIP model from %s on %s", model_source, self.device)

        self.processor = AutoProcessor.from_pretrained(
            model_source,
            cache_dir=str(MODEL_DIR),
            local_files_only=model_source == str(MODEL_DIR),
        )
        self.model = SiglipModel.from_pretrained(
            model_source,
            cache_dir=str(MODEL_DIR),
            local_files_only=model_source == str(MODEL_DIR),
        )
        self.model.to(self.device)
        self.model.eval()

        if model_source != str(MODEL_DIR):
            self.processor.save_pretrained(MODEL_DIR)
            self.model.save_pretrained(MODEL_DIR)
            logger.info("Saved SigLIP model to %s", MODEL_DIR)

    def embed_image(self, image_source: str) -> List[float]:
        if not image_source or not image_source.strip():
            raise ValueError("imageUrl cannot be blank")

        image = self._load_image(image_source.strip())
        inputs = self.processor(images=image, return_tensors="pt")
        inputs = {key: value.to(self.device) for key, value in inputs.items()}

        with torch.no_grad():
            features = self.model.get_image_features(**inputs)
            normalized = torch.nn.functional.normalize(features, p=2, dim=-1)

        return normalized[0].detach().cpu().tolist()

    def embed_text(self, text: str) -> List[float]:
        if not text or not text.strip():
            raise ValueError("text cannot be blank")

        inputs = self.processor(text=[text.strip()], padding="max_length", truncation=True, return_tensors="pt")
        inputs = {key: value.to(self.device) for key, value in inputs.items()}

        with torch.no_grad():
            features = self.model.get_text_features(**inputs)
            normalized = torch.nn.functional.normalize(features, p=2, dim=-1)

        return normalized[0].detach().cpu().tolist()

    def _load_image(self, image_source: str) -> Image.Image:
        if image_source.startswith("data:"):
            return self._load_data_url(image_source)
        if image_source.startswith("http://") or image_source.startswith("https://"):
            return self._load_remote_image(image_source)
        if image_source.startswith("file://"):
            return self._load_local_image(Path(image_source[7:]))
        return self._load_local_image(Path(image_source))

    def _load_remote_image(self, image_url: str) -> Image.Image:
        response = requests.get(image_url, timeout=REQUEST_TIMEOUT_SECONDS)
        response.raise_for_status()
        return Image.open(io.BytesIO(response.content)).convert("RGB")

    def _load_local_image(self, image_path: Path) -> Image.Image:
        if not image_path.exists():
            raise FileNotFoundError(f"Image not found: {image_path}")
        return Image.open(image_path).convert("RGB")

    def _load_data_url(self, data_url: str) -> Image.Image:
        try:
            _, encoded = data_url.split(",", 1)
        except ValueError as exc:
            raise ValueError("Invalid data URL") from exc
        decoded = base64.b64decode(encoded)
        return Image.open(io.BytesIO(decoded)).convert("RGB")


siglip_service = SiglipService()


@asynccontextmanager
async def lifespan(_: FastAPI):
    siglip_service.load()
    yield


app = FastAPI(title="SigLIP Embedding Service", version="1.0.0", lifespan=lifespan)


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        model=MODEL_NAME,
        device=siglip_service.device,
    )


@app.post("/embedding/image", response_model=EmbeddingResponse)
def embedding_image(request: EmbeddingRequest) -> EmbeddingResponse:
    try:
        vector = siglip_service.embed_image(request.imageUrl or "")
        return EmbeddingResponse(vector=vector)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except FileNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except requests.RequestException as exc:
        raise HTTPException(status_code=502, detail=f"Failed to fetch image: {exc}") from exc
    except Exception as exc:
        logger.exception("Failed to embed image")
        raise HTTPException(status_code=500, detail=f"SigLIP image embedding failed: {exc}") from exc


@app.post("/embedding/text", response_model=EmbeddingResponse)
def embedding_text(request: EmbeddingRequest) -> EmbeddingResponse:
    try:
        vector = siglip_service.embed_text(request.text or "")
        return EmbeddingResponse(vector=vector)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        logger.exception("Failed to embed text")
        raise HTTPException(status_code=500, detail=f"SigLIP text embedding failed: {exc}") from exc


if __name__ == "__main__":
    uvicorn.run("app:app", host=HOST, port=PORT, reload=False)
