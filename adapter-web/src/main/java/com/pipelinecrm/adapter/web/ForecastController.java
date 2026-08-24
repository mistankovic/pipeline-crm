package com.pipelinecrm.adapter.web;

import com.pipelinecrm.adapter.web.dto.ApiDtos;
import com.pipelinecrm.adapter.web.dto.Responses;
import com.pipelinecrm.application.port.in.ForecastUseCase;
import com.pipelinecrm.domain.forecast.ForecastBucket;
import java.util.Currency;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final ForecastUseCase forecast;

    public ForecastController(ForecastUseCase forecast) {
        this.forecast = forecast;
    }

    @GetMapping
    public List<ApiDtos.ForecastResponse> get(
            @RequestParam String groupBy, @RequestParam(defaultValue = "USD") String currency) {
        Currency code = Currency.getInstance(currency);
        List<ForecastBucket> buckets =
                "stage".equals(groupBy) ? forecast.byStage(code) : forecast.byOwner(code);
        return buckets.stream().map(bucket -> Responses.forecast(groupBy, bucket)).toList();
    }
}
