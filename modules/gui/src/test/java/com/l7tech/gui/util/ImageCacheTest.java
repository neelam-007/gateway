package com.l7tech.gui.util;

import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.lang.ref.SoftReference;

import static org.junit.Assert.*;

public class ImageCacheTest {

    private static final String INVALID_IMAGE_PATH = "dummy_not_existing_image_resource_path";
    private static final String VALID_IMAGE_PATH = "dummy_existing_image_resource_path";

    private final ImageCache imageCache = ImageCache.getInstance();
    private Image dummyValidImage;
    private Image dummyDefaultImage;

    @Before
    public void setup() {
        dummyValidImage = createNewImage();
        dummyDefaultImage = createNewImage();

        imageCache.getImageMap().put(VALID_IMAGE_PATH, new SoftReference<>(dummyValidImage));
        imageCache.getImageMap().put(ImageCache.DEFAULT_IMAGE_NAME, new SoftReference<>(dummyDefaultImage));
    }

    @BugId("DE221350")
    @Test
    public void testInvalidImageName() throws NullPointerException{
        final Image image = imageCache.getIcon(INVALID_IMAGE_PATH);
        assertEquals("The default image will be returned if the image is invalid.", image, dummyDefaultImage);
    }

    @BugId("DE221350")
    @Test
    public void testValidImageName() {
        final Image image = imageCache.getIcon(VALID_IMAGE_PATH);
        assertEquals("The valid image will be returned.", image, dummyValidImage);
    }

    private Image createNewImage() {
        return new Image() {
            @Override
            public int getWidth(ImageObserver observer) {
                return 0;
            }

            @Override
            public int getHeight(ImageObserver observer) {
                return 0;
            }

            @Override
            public ImageProducer getSource() {
                return null;
            }

            @Override
            public Graphics getGraphics() {
                return null;
            }

            @Override
            public Object getProperty(String name, ImageObserver observer) {
                return null;
            }
        };
    }
}