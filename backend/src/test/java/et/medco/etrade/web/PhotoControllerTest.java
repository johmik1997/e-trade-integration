package et.medco.etrade.web;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import et.medco.etrade.TestProperties;
import et.medco.etrade.config.EtradeProperties;
import et.medco.etrade.service.PhotoStore;
import et.medco.etrade.service.StoredPhoto;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PhotoController.class)
@Import(PhotoControllerTest.Properties.class)
class PhotoControllerTest {

    @TestConfiguration
    static class Properties {
        @Bean
        EtradeProperties etradeProperties() {
            return TestProperties.properties();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PhotoStore photoStore;

    @Test
    void servesStoredBytesWithTheDetectedTypeAndAContentEtag() throws Exception {
        byte[] bytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1};
        given(photoStore.find("abc123")).willReturn(Optional.of(new StoredPhoto("abc123", "image/jpeg", bytes)));

        mockMvc.perform(get("/api/v1/photos/abc123"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(bytes))
                .andExpect(header().string("ETag", "\"abc123\""));
    }

    @Test
    void answers404OnceAPhotoHasExpired() throws Exception {
        given(photoStore.find("gone")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/photos/gone"))
                .andExpect(status().isNotFound());
    }
}
