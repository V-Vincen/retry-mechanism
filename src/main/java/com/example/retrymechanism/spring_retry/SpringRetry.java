package com.example.retrymechanism.spring_retry;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Collections;

/**
 * @author vincent
 */
@Slf4j
public class SpringRetry {
    @Data
    class WeatherInfo {
        /**
         * city : 上海
         * cityid : 101190408
         * temp : 23.5
         * WD : 东北风
         * WS : 小于3级
         * SD : 80%
         * AP : 1006.4hPa
         * njd : 2903
         * WSE : <3
         * time : 17:00
         * sm : 1.1
         * isRadar : 1
         * Radar : JC_RADAR_AZ9210_JB)!!!
         */
        private String city;
        private String cityid;
        private String temp;
        private String WD;
        private String WS;
        private String SD;
        private String AP;
        private String njd;
        private String WSE;
        private String time;
        private String sm;
        private String isRadar;
        private String Radar;
    }

    /**
     * 调取上海当前天气的情况
     *
     * @return
     */
    public WeatherInfo getWeather() {
        //上海区域编码
        String AreaCode = "101020100";
        String url = "http://www.weather.com.cn/data/sk/" + AreaCode + ".html";
        //这里用的是 hutool 工具类，进行 HttpClient 请求和 Json 转化
        String json = HttpUtil.get(url, CharsetUtil.CHARSET_UTF_8);
        JSON parse = JSONUtil.parse(json);
        WeatherInfo weatherinfo = parse.getByPath("weatherinfo", WeatherInfo.class);
        return weatherinfo;
    }

    /**
     * @throws Throwable
     */
    public WeatherInfo retryDoSomething() throws Throwable {
        // 构建重试模板实例
        RetryTemplate retryTemplate = new RetryTemplate();
        // 设置重试策略，主要设置重试次数
        SimpleRetryPolicy policy = new SimpleRetryPolicy(3, Collections.singletonMap(Exception.class, true));
        // 设置重试回退操作策略，主要设置重试间隔时间
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(100);

        retryTemplate.setRetryPolicy(policy);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        log.debug("开始调取外部接口......");
        WeatherInfo execute = retryTemplate.execute(
                new RetryCallback<WeatherInfo, Throwable>() {
                    // 通过RetryCallback 重试回调实例包装正常逻辑逻辑，第一次执行和重试执行执行的都是这段逻辑
                    @Override
                    public WeatherInfo doWithRetry(RetryContext retryContext) throws Throwable {
                        //RetryContext 重试操作上下文约定，统一 spring-try 包装
                        log.debug("第 {} 次重试.....", retryContext.getRetryCount());
                        WeatherInfo weather = null;
                        try {
                            weather = getWeather();

                            //模拟调取外部接口时发生异常
//                            int a = 1 / 0;
                        } catch (Exception e) {
//                            e.printStackTrace();
                            log.error("捕获到的异常{}......", e.getMessage());
                            throw new RuntimeException();//这个点特别注意，重试的根源通过 Exception 返回
                        }
                        return weather;
                    }
                },
                new RecoveryCallback<WeatherInfo>() {
                    // 通过 RecoveryCallback 重试流程正常结束或者达到重试上限后的退出恢复操作实例
                    @Override
                    public WeatherInfo recover(RetryContext retryContext) throws Exception {
                        log.info("do recory operation...");
                        return null;
                    }
                });
        log.debug("调取外部接口结束......");

        return execute;
    }

    @Test
    public void t2() throws Throwable {
        WeatherInfo weatherInfo = retryDoSomething();
        log.info("调用返回的天气情况 {}!!!", weatherInfo);
        log.info("go on with something......");
    }
}



