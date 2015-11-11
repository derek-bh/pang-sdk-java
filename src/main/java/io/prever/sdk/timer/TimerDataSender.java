/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 Preversoft
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.prever.sdk.timer;

import io.prever.sdk.callback.DataCallback;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimerDataSender {
  private static final Logger logger = LoggerFactory.getLogger(TimerDataSender.class);

  private ScheduledThreadPoolExecutor executor;

  private int count;

  private ScheduledFuture<?> scheduledFuture;

  private DataCallback dataCallback;

  private TimeUnit unit;

  private long period;

  private TimerTask task;

  public void setTask(TimerTask task, long period, TimeUnit unit) {
    this.task = task;
    this.period = period;
    this.unit = unit;
  }

  public void setDataCallback(DataCallback dataCallback) {
    this.dataCallback = dataCallback;
  }

  public void cancel() {
    if (scheduledFuture == null || scheduledFuture.isCancelled()) {
      return;
    }

    if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
      scheduledFuture.cancel(false);
      scheduledFuture = null;
      logger.info("DataSending task is stopped.");
    }
  }

  public void await() {
    if (scheduledFuture == null) {
      return;
    }

    try {
      scheduledFuture.get();
    } catch (Exception e) {
    }
  }

  public void await(long timeout, TimeUnit unit) {
    if (scheduledFuture == null) {
      return;
    }

    try {
      scheduledFuture.get(timeout, unit);
    } catch (Exception e) {
      cancel();
    }
  }

  public void start() {
    if (task == null) {
      throw new IllegalStateException("TimeTask must not be null");
    }

    available();

    count = 0;
    
    if (executor == null) {
      executor = new ScheduledThreadPoolExecutor(1);
    }

    scheduledFuture = executor.scheduleAtFixedRate(new Runnable() {
      public void run() {
        if (dataCallback != null) {
          count++;

          if (!dataCallback.isRunning(count)) {
            cancel();
            return;
          }

          try {
            Object data = dataCallback.getData();
            if (data != null) {
              boolean sendData = task.execute(data);
              if (sendData) {
                dataCallback.onSuccess(data);
              }
            }
          } catch (Exception e) {
            logger.error("TimerTask error", e);
          }
        }
      }
    }, 2, period, unit);

    logger.info("TimerDataSender is started.");
  }

  public void available() {
    if (scheduledFuture != null) {
      if (!scheduledFuture.isCancelled()) {
        throw new IllegalStateException("TimerDataSender is already started.");
      }
    }
  }

  public void destroy() {
    cancel();
    if (executor != null && !executor.isShutdown()) {
      executor.shutdown();
      logger.info("Task executor is shutting down.");
    }
  }

}
