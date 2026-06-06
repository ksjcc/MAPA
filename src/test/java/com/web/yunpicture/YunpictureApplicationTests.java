package com.web.yunpicture;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.web.yunpicturebackend.YunpictureBackendApplication;

@ActiveProfiles("local")
@SpringBootTest(classes = YunpictureBackendApplication.class)
class YunpictureApplicationTests {

    @Test
    void contextLoads() {
    }

}
