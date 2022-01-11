/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kernel;

import java.io.IOException;
import static java.util.Objects.requireNonNull;
import java.util.Set;

import org.kernel.Communication;
import org.kernel.Request;
import org.kernel.Response;

import org.httpclient.OrderPollingJob;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 *
 * @author zzw
 */

public class KernelStart {

//    public static void main(String args[]){
//        Request request = new Request("Order and State Request Thread");
//        Response response = new Response("Order and State Response Thread");
//        
//        Thread reqt = new Thread(request);
//        Thread rest = new Thread(response);
//        
//        reqt.start();
//        rest.start();
//    }
    
     public static void main(String[] args) throws SchedulerException {
        //创建任务
        JobDetail jobDetail = JobBuilder.newJob(OrderPollingJob.class).withIdentity("job1", "group1").build();
        //创建触发器 每10秒钟执行一次
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger1", "group3")
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(10).repeatForever())
                        .build();
        //创建调度器
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        Scheduler scheduler = schedulerFactory.getScheduler();
        //将任务及其触发器放入调度器
        scheduler.scheduleJob(jobDetail, trigger);
        //调度器开始调度任务
        scheduler.start();
    }
    
}
