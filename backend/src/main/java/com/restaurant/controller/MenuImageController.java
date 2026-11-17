package com.restaurant.controller;

import com.restaurant.exception.NotFoundException;
import com.restaurant.service.MenuImageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MenuImageController {
  private final MenuImageService images;

  public MenuImageController(MenuImageService images) {
    this.images = images;
  }

  @GetMapping("/images/menu/uploads/{id}")
  public ResponseEntity<byte[]> image(@PathVariable long id) {
    try {
      var photo = images.read(id);
      return ResponseEntity.ok().cacheControl(CacheControl.noCache())
          .contentType(MediaType.parseMediaType(photo.contentType()))
          .body(photo.bytes());
    } catch (NotFoundException ex) {
      return ResponseEntity.notFound().build();
    }
  }
}
