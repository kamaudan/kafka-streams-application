package io.github.kamaudan.KafkaAnalytics;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
class PageViewEvent {


    private String userId , page;
    private long duration;

}