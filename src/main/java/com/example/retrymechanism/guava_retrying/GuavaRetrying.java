package com.example.retrymechanism.guava_retrying;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.AttemptTimeLimiters;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author vincent
 */
@Slf4j
public class GuavaRetrying {
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
     * Guava retryer: 重试机制
     * .retryIfException() -> 抛出异常会进行重试
     * .retryIfResult(Predicates.equalTo(false)) -> 如果接口返回的结果不符合预期,也需要重试
     * .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS)) -> 重试策略, 此处设置的是重试间隔时间
     * .withStopStrategy(StopStrategies.stopAfterAttempt(3)) -> 重试次数
     * .withAttemptTimeLimiter(AttemptTimeLimiters.fixedTimeLimit(2, TimeUnit.SECONDS)) -> 某次请求不得超过2s，
     *                        使用这个属性会报 "java.lang.NoSuchMethodError: com.google.common.util.concurrent.SimpleTimeLimiter: method <init>()V not found" 个人感觉是 jar 暂时没找到原因
     */
    private Retryer<WeatherInfo> retryer = RetryerBuilder.<WeatherInfo>newBuilder()
            .retryIfException()
            .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(3))
//            .withAttemptTimeLimiter(AttemptTimeLimiters.fixedTimeLimit(3, TimeUnit.SECONDS))
            .withRetryListener(new RetryListener() {
                @Override
                public <WeatherInfo> void onRetry(Attempt<WeatherInfo> attempt) {
                    // attempt.getAttemptNumber() -> 第几次重试,(注意:第一次重试其实是第一次调用)
                    log.debug("第 retryTime = {}次重试......", attempt.getAttemptNumber());
                    // attempt.getDelaySinceFirstAttempt() -> 距离第一次重试的延迟
                    log.debug("距离上一次重试的 delay = {}......", attempt.getDelaySinceFirstAttempt());
                    if (attempt.hasException()) {
                        // attempt.hasException() -> 是异常终止
                        log.debug("异常终止 causeBy = {}......", attempt.getExceptionCause().toString());
                    } else {
                        // attempt.hasResult() -> 是正常返回
                        log.debug("成功获取结果为 {}......", attempt.getResult());
                    }
                }
            })
            .build();

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

        //模拟调取外部接口时发生异常
        int a = 1 / 0;
        return weatherinfo;
    }

    @Test
    public void t2() throws Throwable {
        log.debug("开始调取外部接口......");
        WeatherInfo weatherInfo = retryer.call(() -> getWeather());
        log.debug("调取外部接口结束......");
        log.info("调用返回的天气情况 {}!!!", weatherInfo);
        log.info("go on with something......");
    }
}
