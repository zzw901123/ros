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
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.util.time.TimeZoneFormat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import com.fasterxml.jackson.datatype:jackson-datatype-jsr310;

import org.httpclient.v1.telegrams.RequestResponse;

import org.httpclient.v1.order.binding.Property;
import org.httpclient.v1.order.binding.Destination;
import org.httpclient.v1.order.binding.Transport;

public class OrderPollingJob implements Job {
    // 
    static String host = "http://127.0.0.1:";
    static String port = "55200";
    static String url = "/v1/hello";
    
    static int chinaZoneTime = 8;
    
    static String placeOrderSucessResponse = "Successful operation";
    
    // 
    String requestOrderResponse;
    String placeOrderResponse;
    
    @Override
    public void execute(JobExecutionContext arg) throws JobExecutionException {
        
        //return order(data type:String ) through get request(Http)
        String geturl = host + port + url;
        requestOrderResponse = HttpClientUtils.sendGetRequest(geturl,"UTF-8");
        System.out.println(requestOrderResponse);
        
        //Combine orders into corresponding data structures
        
        //Placing orders Json->url
        // if there is no order, return
        if(requestOrderResponse == null){
            return;
        }
        // place order to openTcs through post 
        else{
            try {
                //Parsing request response
                //simulication requestOrderResponse
                
                //Init
                RequestResponse reqres = new RequestResponse();
                
                Transport transport = new Transport();
                
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                
                Instant deadline = null;
                String intendedVehicle;
                List<Destination> destinations = new LinkedList<>();
                List<Property> properties = new LinkedList<>();
                List<String> dependencies = new LinkedList<>();
                
                // Order deadline: get current time, offsetTime = 2h
                // TODO Estimate the time according to the amount of tasks
                deadline = Instant.ofEpochMilli(new Date(System.currentTimeMillis() + 120*60*1000).getTime());
                System.out.println(deadline);
                
                // The (optional) intended vehicle of the transport order
                // TODO According to the actual number of cars
                Random rand = new Random();
                int vechicleNum = rand.nextInt(4+1);
                if(vechicleNum < 10){
                    intendedVehicle = "Vehicle-0" + String.valueOf(vechicleNum);    
                }
                else{
                    intendedVehicle = "Vehicle-" + String.valueOf(vechicleNum);
                }
                System.out.println(intendedVehicle);
                
                
                //The destinations
                // TODO need use json
                requestOrderResponse = "{\"orderNum\":\"R202106101735\",\"loadingLocation\":\"Point-0034\",\"unloadingLocation\":\"Point-0110\"}";
                reqres = mapper.readValue(requestOrderResponse, RequestResponse.class);
                System.out.println(reqres);
                
                //Under the current situation, it is set to move
                Destination destination = new Destination();
                destination.setLocationName(reqres.getLoadingLocation());
                destination.setOperation("MOVE");
                destinations.add(destination);
                destination.setLocationName(reqres.getUnloadingLocation());
                destination.setOperation("MOVE");
                destinations.add(destination);
                
                // Combined order TODO
                transport.setDeadline(deadline);
                transport.setIntendedVehicle(intendedVehicle);
                transport.setDestinations(destinations);
                transport.setProperties(properties);
                transport.setDependencies(dependencies);
                
                //Combined body
                String json = mapper.writeValueAsString(transport);
                System.out.println(json);  

                //Combined URL
                String url1 = "/v1/transportOrders/" + reqres.getOrderNum();
                String requestopenTcsUrl = host + port + url1;
                System.out.println(requestopenTcsUrl);
                
                try {
                    placeOrderResponse = HttpClientUtils.sendPostByJson(requestopenTcsUrl, json);
                } catch (Exception ex) {
                    Logger.getLogger(OrderPollingJob.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            } catch (JsonProcessingException ex) {
                Logger.getLogger(OrderPollingJob.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        //place order sucess or fail, 
        if(placeOrderResponse == null ? placeOrderSucessResponse == null : placeOrderResponse.equals(placeOrderSucessResponse)){
            
        }
        else{
            
        }
           
    }
}