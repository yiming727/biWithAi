package com.yiming.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.yiming.springbootinit.DeepSeek.DeepSeekService;
import com.yiming.springbootinit.common.ErrorCode;
import com.yiming.springbootinit.exception.BusinessException;
import com.yiming.springbootinit.manager.AiManager;
import com.yiming.springbootinit.model.entity.Chart;
import com.yiming.springbootinit.service.ChartService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

// 使用@Component注解标记该类为一个组件，让Spring框架能够扫描并将其纳入管理
@Component
// 使用@Slf4j注解生成日志记录器
@Slf4j
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    @Resource
    private DeepSeekService deepSeekService;

    /**
     * 接收消息的方法
     *
     * @param message      接收到的消息内容，是一个字符串类型
     * @param channel      消息所在的通道，可以通过该通道与 RabbitMQ 进行交互，例如手动确认消息、拒绝消息等
     * @param deliveryTag  消息的投递标签，用于唯一标识一条消息
     */
    // 使用@SneakyThrows注解简化异常处理
    @SneakyThrows
    // 使用@RabbitListener注解指定要监听的队列名称为"code_queue"，并设置消息的确认机制为手动确认
    @RabbitListener(queues = {"code_queue"}, ackMode = "MANUAL")
    // @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag是一个方法参数注解,用于从消息头中获取投递标签(deliveryTag),
    // 在RabbitMQ中,每条消息都会被分配一个唯一的投递标签，用于标识该消息在通道中的投递状态和顺序。通过使用@Header(AmqpHeaders.DELIVERY_TAG)注解,可以从消息头中提取出该投递标签,并将其赋值给long deliveryTag参数。
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        if (StringUtils.isBlank(message)){
            //如果失败，消息拒绝
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表不存在");
        }
        //先修改图表任务状态为执行中
        Chart updateChart =  new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
        if (!b) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
            return;
        }
//        // 调用Ai
//        String result = aiManager.doChat(CommonConstant.BI_MODEL_ID,buildUserInput(chart));
        //调用deepseek
        String result = deepSeekService.callDeepSeek(buildUserInput(chart));
        // 对返回结果做拆分,按照5个中括号进行拆分
        String[] splits = result.split("【【【【【");
        // 拆分之后还要进行校验
        if (splits.length < 3) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "AI 生成错误");
            return;
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        updateChartResult.setStatus("succeed");
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
        }
        // 手动确认消息的接收，向RabbitMQ发送确认消息
        channel.basicAck(deliveryTag, false);
    }

    /**
     * 构造用户输入
     * @param chart
     * @return
     */

    private String buildUserInput(Chart chart) {
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();
        final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                "分析需求：\n" +
                "{数据分析的需求或者目标}\n" +
                "原始数据：\n" +
                "{csv格式的原始数据，用,作为分隔符}\n" +
                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                "【【【【【\n" +
                "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
                "【【【【【\n" +
                "{明确的数据分析结论、越详细越好，不要生成多余的注释}" +
                "下面是一个前端 Echarts V5 的 option 配置对象js代码示例：{\n" +
                "    \"title\": {\n" +
                "        \"text\": \"用户数趋势\"\n" +
                "    },\n" +
                "    \"tooltip\": {\n" +
                "        \"trigger\": \"axis\"\n" +
                "    },\n" +
                "    \"legend\": {\n" +
                "        \"data\": [\"用户数\"]\n" +
                "    },\n" +
                "    \"grid\": {\n" +
                "        \"left\": \"3%\",\n" +
                "        \"right\": \"4%\",\n" +
                "        \"bottom\": \"3%\",\n" +
                "        \"containLabel\": true\n" +
                "    },\n" +
                "    \"xAxis\": {\n" +
                "        \"type\": \"category\",\n" +
                "        \"boundaryGap\": false,\n" +
                "        \"data\": [\"1号\", \"2号\", \"3号\"]\n" +
                "    },\n" +
                "    \"yAxis\": {\n" +
                "        \"type\": \"value\"\n" +
                "    },\n" +
                "    \"series\": [\n" +
                "        {\n" +
                "            \"name\": \"用户数\",\n" +
                "            \"type\": \"line\",\n" +
                "            \"stack\": \"总量\",\n" +
                "            \"data\": [10, 20, 30]\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("【【【【【\n").append(prompt).append("\n");
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        // 如果图表类型不为空
        if (StringUtils.isNotBlank(chartType)) {
            // 就将分析目标拼接上“请使用”+图表类型
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");
        return userInput.toString();
    }

    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus("failed");
        updateChart.setExecMessage("execMessage");
        boolean b = chartService.updateById(updateChart);
        if (!b){
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }

}
