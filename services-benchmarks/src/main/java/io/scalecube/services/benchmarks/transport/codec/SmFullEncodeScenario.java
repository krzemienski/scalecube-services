package io.scalecube.services.benchmarks.transport.codec;

import io.scalecube.benchmarks.BenchmarkSettings;
import io.scalecube.benchmarks.BenchmarkState;
import io.scalecube.benchmarks.metrics.BenchmarkMeter;
import io.scalecube.benchmarks.metrics.BenchmarkTimer;
import io.scalecube.benchmarks.metrics.BenchmarkTimer.Context;
import io.scalecube.services.api.ServiceMessage;
import io.scalecube.services.transport.api.ReferenceCountUtil;
import io.scalecube.services.transport.api.ServiceMessageCodec;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class SmFullEncodeScenario {

  private SmFullEncodeScenario() {
    // Do not instantiate
  }

  /**
   * Runner function for benchmarks.
   *
   * @param args program arguments
   * @param benchmarkStateFactory producer function for {@link BenchmarkState}
   */
  public static void runWith(
      String[] args, Function<BenchmarkSettings, SmCodecBenchmarkState> benchmarkStateFactory) {

    BenchmarkSettings settings =
        BenchmarkSettings.from(args).durationUnit(TimeUnit.NANOSECONDS).build();

    SmCodecBenchmarkState benchmarkState = benchmarkStateFactory.apply(settings);

    benchmarkState.runForSync(
        state -> {
          BenchmarkTimer timer = state.timer("timer");
          BenchmarkMeter meter = state.meter("meter");
          ServiceMessageCodec messageCodec = state.messageCodec();
          ServiceMessage message = state.message();

          return i -> {
            Context timeContext = timer.time();
            Object result =
                messageCodec.encodeAndTransform(
                    message,
                    (dataByteBuf, headersByteBuf) -> {
                      ReferenceCountUtil.safestRelease(dataByteBuf);
                      ReferenceCountUtil.safestRelease(headersByteBuf);
                      return dataByteBuf;
                    });
            timeContext.stop();
            meter.mark();
            return result;
          };
        });
  }
}
