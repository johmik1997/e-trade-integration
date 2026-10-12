package et.medco.etrade.web;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import org.springframework.http.HttpStatus;

import et.medco.etrade.service.PhotoStore;
import et.medco.etrade.service.StoredPhoto;

/**
 * Serves associate photos that were lifted out of the upstream payload. Ids are content
 * hashes, so these responses are safe to cache hard and to ETag by id.
 */
@RestController
@RequestMapping("/api/v1/photos")
public class PhotoController {

    private final PhotoStore photoStore;

    PhotoController(PhotoStore photoStore) {
        this.photoStore = photoStore;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> photo(@PathVariable String id) {
        StoredPhoto photo = photoStore.find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No photo with id " + id + " is held; re-request the registration to repopulate it"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .eTag('"' + photo.id() + '"')
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate().immutable())
                .body(photo.bytes());
    }
}
