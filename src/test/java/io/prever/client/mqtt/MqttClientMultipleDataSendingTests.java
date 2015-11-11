package io.prever.client.mqtt;

import io.prever.client.domain.Sensor;
import io.prever.sdk.Prever;
import io.prever.sdk.callback.ConnectionCallback;
import io.prever.sdk.callback.MultipleDataCallback;
import io.prever.sdk.mqtt.PDefaultMqttClient;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MqttClientMultipleDataSendingTests {

  private Prever sendingDataClient;

  Sensor sensor = new Sensor();

  @Before
  public void init() throws Exception {
    sendingDataClient = new PDefaultMqttClient("josh", "mykey");
    final CountDownLatch latch = new CountDownLatch(1);
    sendingDataClient.setConnectionCallback(new ConnectionCallback() {

      public void onConnectionSuccess() {
        latch.countDown();
      }

      public void onConnectionLost(Throwable cause) {}

      public void onConnectionFailure(Throwable cause) {}
    });
    sendingDataClient.connect("tcp://localhost");
    latch.await(3, TimeUnit.SECONDS);
  }


  @Test
  public void periodicSendDataTest() {
    final int MAX_COUNT = 2;
    final CountDownLatch countDownLatch = new CountDownLatch(MAX_COUNT);

    sendingDataClient.startTimerTask(new MultipleDataCallback() {

      
      public boolean isRunning(int count) {
        System.out.println("count : " + count);
        countDownLatch.countDown();
        return count < MAX_COUNT;
      }

      public void onSuccess(Object value) {
        
      }

      public Object getData() {
          sensor.setHumidity((int) (Math.random() * 30 + 30));
          sensor.setTemperature((int) (Math.random() * 20 + 20));
          sensor.setTimeStamp(new Date());
        return sensor;
      }
      
    }, 5, TimeUnit.SECONDS);

    try {
      countDownLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
      Assert.fail();
    } finally {
      Assert.assertEquals(0, countDownLatch.getCount());
      sendingDataClient.disconnect();
    }
  }
  
  @Test
  public void periodicSendMapDataTest() {
    final int MAX_COUNT = 2;
    final CountDownLatch countDownLatch = new CountDownLatch(MAX_COUNT);
    
    sendingDataClient.startTimerTask(new MultipleDataCallback() {
      
      
      public boolean isRunning(int count) {
        System.out.println("count : " + count);
        countDownLatch.countDown();
        return count < MAX_COUNT;
      }
      
      public void onSuccess(Object value) {
        
      }
      
      public Object getData() {
        Map<String, Object> data = new HashMap<String, Object> ();
        data.put("humidity", (int) 0);
        data.put("temperature", (int) (Math.random() * 20 + 20));
        data.put("timeStamp", new Date().getTime());
        data.put("key", "123");
        return data;
      }
      
    }, 5, TimeUnit.SECONDS);
    
    try {
      countDownLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
      Assert.fail();
    } finally {
      Assert.assertEquals(0, countDownLatch.getCount());
      sendingDataClient.disconnect();
    }
  }

}
