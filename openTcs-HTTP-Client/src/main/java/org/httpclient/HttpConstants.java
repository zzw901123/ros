/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.httpclient;

/**
 *
 * @author zzw
 */
public interface HttpConstants {
    
    /**
   * Name of the header that is expected to contain the API access keys.
   */
  String HEADER_NAME_ACCESS_KEY = "X-Api-Access-Key";
  /**
   * Content type for plain text.
   */
  String CONTENT_TYPE_TEXT_PLAIN_UTF8 = "text/plain; charset=utf-8";
  /**
   * Content type for JSON structures.
   */
  String CONTENT_TYPE_APPLICATION_JSON_UTF8 = "application/json; charset=utf-8";

}
