package com.web.yunpicturebackend.config;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.web.yunpicturebackend.manager.websocket.PictureEditHandler;
import com.web.yunpicturebackend.manager.websocket.disruptor.PictureEditEvent;
import com.web.yunpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.web.yunpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.concurrent.Executors;

@Configuration
@Slf4j
public class PictureEditDisruptorConfig {

    private static final int RING_BUFFER_SIZE = 1024;

    @Bean
    public EventHandler<PictureEditEvent> pictureEditEventHandler(@Lazy PictureEditHandler pictureEditHandler) {
        return (event, sequence, endOfBatch) -> {
            PictureEditRequestMessage message = event.getPictureEditRequestMessage();
            if (message == null) {
                return;
            }
            PictureEditMessageTypeEnum typeEnum = PictureEditMessageTypeEnum.getEnumByValue(message.getType());
            if (typeEnum == null) {
                log.warn("Unsupported picture edit message type: {}", message.getType());
                return;
            }
            switch (typeEnum) {
                case ENTER_EDIT:
                    pictureEditHandler.handleEnterEditMessage(message, event.getSession(), event.getUser(),
                            event.getPictureId());
                    break;
                case EXIT_EDIT:
                    pictureEditHandler.handleExitEditMessage(message, event.getSession(), event.getUser(),
                            event.getPictureId());
                    break;
                case EDIT_ACTION:
                    pictureEditHandler.handleEditActionMessage(message, event.getSession(), event.getUser(),
                            event.getPictureId());
                    break;
                default:
                    log.debug("Ignoring picture edit message type: {}", message.getType());
            }
        };
    }

    @Bean
    public Disruptor<PictureEditEvent> pictureEditEventDisruptor(
            EventHandler<PictureEditEvent> pictureEditEventHandler) {
        EventFactory<PictureEditEvent> eventFactory = PictureEditEvent::new;
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(
                eventFactory,
                RING_BUFFER_SIZE,
                Executors.defaultThreadFactory(),
                ProducerType.MULTI,
                new com.lmax.disruptor.BlockingWaitStrategy());
        disruptor.handleEventsWith(pictureEditEventHandler);
        disruptor.start();
        return disruptor;
    }
}
