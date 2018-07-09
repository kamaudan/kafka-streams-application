package io.github.kamaudan.KafkaAnalytics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableBinding(AnalyticsBinding.class)
public class KafkaAnalytics {

    public  static class PageViewSource implements ApplicationRunner {

        private  final MessageChannel pageViewsOut;
        private final Log log = LogFactory.getLog(getClass());

        public PageViewSource(AnalyticsBinding binding) {
            this.pageViewsOut = binding.pageviesout();
        }

        @Override
        public void run(ApplicationArguments args) throws Exception {

            List <String>  names = Arrays.asList("JJ", "Thelma"," Michael", "Willona","Ciddy Thomson");
            List <String>  pages = Arrays.asList("blog","sitemap","news","about", "contact");


            Runnable runnable = () -> {

                String rPage = pages.get(new Random().nextInt(pages.size()));
                String  rnames= names.get(new Random().nextInt(names.size()));

                PageViewEvent pageViewEvent = new PageViewEvent(rnames, rPage, Math.random() > .5 ? 10 : 1000);

                Message<PageViewEvent> message = MessageBuilder
                        .withPayload(pageViewEvent)
                        .setHeader(KafkaHeaders.MESSAGE_KEY, pageViewEvent.getUserId().getBytes())
                        .build();
                try {
                    this.pageViewsOut.send(message);
                }
                catch (Exception e) {
                    log.info(e);

                }

            };

            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(runnable, 1,1, TimeUnit.SECONDS);

        }
    }

    @Component
    public static class PageViewEventSink {

        private  final Log log = LogFactory.getLog(getClass());

        @StreamListener
        @SendTo(AnalyticsBinding.PAGE_COUNT_OUT)
        public KStream<String,Long> process (
                @Input (AnalyticsBinding.PAGE_VIEWS_IN)
                        KStream<String, PageViewEvent> events){

              return events
                    .filter((key, value) -> value.getDuration() > 10)
                    .map((key, value) -> new KeyValue<> (value.getPage(), "0" ))
                    .groupByKey()
                    .count( Materialized.as(AnalyticsBinding.PAGE_COUNT_MV))
                    .toStream();

        }

    }

    @Component
    public static class PageCountSink {

        private final Log log = LogFactory.getLog(getClass());

        @StreamListener
        public void process(@Input (AnalyticsBinding.PAGE_COUNT_IN) KTable <String, Long> counts) {

            counts
                    .toStream()
                    .foreach((key, value)-> log.info(key + "=" + value));


        }



    }


    public static void main(String[] args) {
		SpringApplication.run(KafkaAnalytics.class, args);
	}
}

interface  AnalyticsBinding {

    String PAGE_VIEWS_OUT = "pvout";
    String PAGE_VIEWS_IN = "pvin";
    String PAGE_COUNT_MV = "pcmv";
    String PAGE_COUNT_OUT = "pcout";
    String PAGE_COUNT_IN ="pcin";

    //page view

    @Input(PAGE_VIEWS_IN)
    KStream<String, PageViewEvent> pageViewin();


    @Output(PAGE_COUNT_OUT)
    KStream<String, Long> pageCountOut();


    //page count


    @Input(PAGE_COUNT_IN)
    KTable<String, Long> pageCountIn();


    @Output(PAGE_VIEWS_OUT)
    MessageChannel pageviesout();
}





