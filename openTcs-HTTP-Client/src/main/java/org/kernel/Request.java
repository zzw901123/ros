/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kernel;

import org.kernel.Communication;

/**
 *
 * @author zzw
 */
public class Request extends Communication{

    private String name;
    
    public Request(String name){
        this.name = name;
    }
    
    public void run(){
        while(true){
            
            //解析报文 
            System.out.println(name + "运行！");
            try{ 
                  Thread.sleep(500) ; // 线程休眠 
            }catch(InterruptedException e){    } 
        }
    }
}
