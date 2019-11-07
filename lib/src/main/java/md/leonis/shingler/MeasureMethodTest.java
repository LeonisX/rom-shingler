package md.leonis.shingler;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class MeasureMethodTest {

    public static void main(String[] args) throws InterruptedException {
        premain();
        for (int i = 0; i < 4; i++) {
            SampleClass.foo("arg" + i);
        }
    }

    public static void premain() {
        new AgentBuilder.Default()
                //.type(ElementMatchers.isAnnotatedWith(Measured.class))
                .type(ElementMatchers.nameStartsWith("md.leonis.shingler"))
                .transform((builder, type, classLoader, module) ->
                        builder.method(ElementMatchers.any()).intercept(MethodDelegation.to(AccessInterceptor.class))
                ).installOn(ByteBuddyAgent.install());
    }

    public static class AccessInterceptor {

        @RuntimeType
        public static Object intercept(@Origin Method method, @SuperCall Callable<?> callable, @AllArguments Object[] args) throws Exception {
            long start = System.nanoTime();
            try {
                return callable.call();
            } finally {
                long ms = ((System.nanoTime() - start) / 1000000);
                if (method.getAnnotationsByType(Measured.class).length > 0 && ms > 5) {
                    String params = Arrays.stream(args).map(Object::toString).collect(Collectors.joining(", "));
                    System.out.println(method.getReturnType().getSimpleName() + " " + method.getName() + "("+ params +") took " + ms + " ms");
                }
            }
        }
    }

    public static class SampleClass {
        @Measured
        static void foo(String s) throws InterruptedException {
            Thread.sleep(50);
        }
    }
}
