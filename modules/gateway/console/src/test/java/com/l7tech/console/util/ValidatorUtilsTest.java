package com.l7tech.console.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: ymoiseyenko
 * Date: 11/30/11
 * Time: 10:08 AM
 */
public class ValidatorUtilsTest {

    @Test
    public void shouldReturnFailedMessageWhenPathContainsReserverdWord() throws Exception {
          String[] paths = {"/ssg/invalidPath", "/service/1234556"};
          for(String invalidPath : paths) {
            assertTrue(null != ValidatorUtils.validateResolutionPath(invalidPath, true, false));
          }

    }


    @Test
    public void shouldReturnFailedMessageWhenPathContainsReserverdWordAndServiceIsInternal() throws Exception {

        String[] paths = {"/ssg/invalidPath", "/service/1234556"};
        for(String invalidPath : paths) {
            assertTrue(null != ValidatorUtils.validateResolutionPath(invalidPath, true, true));
        }
    }


    @Test
    public void shouldPassMessageWhenPathIsEmpty() throws Exception {
          String[] paths = {"", "/"};
          for(String path : paths){
            assertTrue(null == ValidatorUtils.validateResolutionPath(path, true, false));
          }
    }


    @Test
    public void shouldReturnFailedMessageWhenPathIsEmptyForNonSoapService() throws Exception {
          String[] paths ={ "/", ""};

          for(String invalidPath : paths){
            assertTrue(null != ValidatorUtils.validateResolutionPath(invalidPath, false, false));
          }

    }


    @Test
    public void shouldReturnFailedMessageWhenPathIsNullForNonSoapService() throws Exception {
        assertTrue(null != ValidatorUtils.validateResolutionPath(null, false, false));
    }


    @Test
    public void shouldPassWhenPathIsNullForNonSoapService() throws  Exception {
        assertTrue(null == ValidatorUtils.validateResolutionPath(null, true, false));
    }

    @Test
    public void shouldReturnFailedMessageWhenPathIsInvalidForNonSoap() throws Exception {
          String[] paths = {"/ssg/invalidpath", "/service/123456"};

          for(String invalidPath : paths){
            assertTrue(null != ValidatorUtils.validateResolutionPath(invalidPath, false, false));
          }
    }


    @Test
    public void shouldPassWhenPathIsValid() throws Exception {
          String validPath = "/somepath";
          assertTrue(null == ValidatorUtils.validateResolutionPath(validPath, true, false));
    }
}
