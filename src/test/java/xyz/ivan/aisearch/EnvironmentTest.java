package xyz.ivan.aisearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import xyz.ivan.aisearch.entity.Merchant;
import xyz.ivan.aisearch.repository.MerchantRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest // 启动 Spring 上下文
class EnvironmentTest {

    @Autowired
    private ChatLanguageModel chatModel;      // 注入 Qwen 对话模型

    @Autowired
    private EmbeddingModel embeddingModel;    // 注入 Qwen 向量模型

    @Autowired
    private MerchantRepository merchantRepo;  // 注入 PG Repository

    @Autowired
    private ElasticsearchClient esClient;     // 注入 ES 客户端

    /**
     * 测试 1: 验证 LangChain4j + 通义千问 API 是否通畅
     */
    @Test
    void testAiConnection() {
        System.out.println("====== 开始测试 AI 连接 ======");

        // 1. 测试对话
        String answer = chatModel.generate("你好，这是一条测试消息，请回复'收到'。");
        System.out.println("AI 回复: " + answer);
        assertNotNull(answer);

        // 2. 测试向量生成
        Response<Embedding> response = embeddingModel.embed("测试文本");
        System.out.println("向量生成成功，维度: " + response.content().dimension());

        // 通义千问 text-embedding-v2 应该是 1536 维
        assertEquals(1536, response.content().dimension());

        System.out.println("====== AI 连接测试通过 ======");
    }

    /**
     * 测试 2: 验证 数据库(PG) + 搜索引擎(ES) 的写入流程
     */
    @Test
    void testDataPipeline() throws IOException {
        System.out.println("====== 开始测试数据写入流程 ======");

        // 1. 模拟一个商户对象
        Merchant merchant = new Merchant();
        merchant.setName("集成测试餐厅-" + System.currentTimeMillis());
        merchant.setCategory("测试菜系");
        merchant.setAvgPrice(100);
        merchant.setRating(new BigDecimal("4.5"));
        merchant.setAddress("测试地址 123 号");
        merchant.setDescription("这这是一条用于测试环境连通性的数据。包含中文分词测试。");
        merchant.setLatitude(39.90);
        merchant.setLongitude(116.40);

        // 2. 写入 PostgreSQL
        Merchant saved = merchantRepo.save(merchant);
        System.out.println("PostgreSQL 写入成功, ID: " + saved.getId());
        assertNotNull(saved.getId());

        // 3. 生成向量
        String textToEmbed = merchant.getCategory() + "," + merchant.getDescription();
        Embedding embedding = embeddingModel.embed(textToEmbed).content();

        // 4. 写入 Elasticsearch
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", saved.getId());
        doc.put("name", saved.getName());
        doc.put("category", saved.getCategory());
        doc.put("avg_price", saved.getAvgPrice());
        doc.put("rating", saved.getRating());
        doc.put("address", saved.getAddress());
        doc.put("description", saved.getDescription());
        doc.put("location", Map.of("lat", saved.getLatitude(), "lon", saved.getLongitude()));
        doc.put("description_vector", embedding.vectorAsList()); // 关键向量字段

        IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                .index("merchants") // 索引名，必须和你之前 PUT 的一致
                .id(String.valueOf(saved.getId()))
                .document(doc)
        );

        esClient.index(request);
        System.out.println("Elasticsearch 写入指令已发送");

        System.out.println("====== 数据写入测试通过，请去 Kibana 验证 ======");
    }
}
