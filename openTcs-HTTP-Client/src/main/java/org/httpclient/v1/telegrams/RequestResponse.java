/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.httpclient.v1.telegrams;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Size;

/**
 *
 * @author zzw
 */
public class RequestResponse {
    @JsonPropertyDescription("The serial number of the transport order")
    @JsonProperty(required = true)
    private String orderNum;
    
    @JsonPropertyDescription("The loading location")
    private String loadingLocation;
    
    @JsonPropertyDescription("The unloading location")
    private String unloadingLocation;
    
    /**
   * Creates a new instance.
   */
    public RequestResponse() {
    }
    
    public void setOrderNum(String orderNum)
    {
        this.orderNum = orderNum;
    }
    public String getOrderNum()
    {
        return this.orderNum;
    }
    
    public void setLoadingLocation(String loadingLocation){
        this.loadingLocation = loadingLocation;
    }
    
    public String getLoadingLocation(){
        return this.loadingLocation;
    }
    
    public void setUnloadingLocation(String unloadingLocation){
        this.unloadingLocation = unloadingLocation;
    }
    
    public String getUnloadingLocation(){
        return this.unloadingLocation;
    }
}
