package com.example.demo.anti.proxy;

import com.example.demo.utils.SleepTools;

import java.util.Iterator;

public class TestCircleBlockingQueue {
    static CircleBlockingQueue<String> queue  = new CircleBlockingQueue<>(100);

    static class AddThread implements Runnable {
        @Override
        public void run() {
            int count = 0;
            while(true){
                try {
                    SleepTools.sleep(10);
                    count += 1;
                    queue.put(count + "");
                    System.out.println("put " + count);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class DelThread implements Runnable {
        @Override
        public void run() {
            while(true){
                try {
                    String take = queue.take();
                    System.out.println("del " + take);
                    SleepTools.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
//        new Thread(new AddThread()).start();
        //new Thread(new DelThread()).start();
        queue.put("1");
        queue.put("2");
        queue.put("3");
        queue.put("4");
        queue.put("5");
        queue.put("6");
        Iterator<String> iterator = queue.iterator();
        String next = iterator.next();
        while(next!=null){
            System.out.println(next);
        }
    }
}
