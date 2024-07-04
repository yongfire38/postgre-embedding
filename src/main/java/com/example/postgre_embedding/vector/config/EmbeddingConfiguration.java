package com.example.postgre_embedding.vector.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class EmbeddingConfiguration {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingConfiguration.class);

    @Value("vectorstore.json")
    private String vectorStoreName;

    @Value("classpath:/docs/bread.txt")
    private Resource resource;

    @Bean
    VectorStore pgVectorStore(JdbcTemplate jdbcTemplate, TransformersEmbeddingModel embeddingModel) throws Exception {

        embeddingModel.setTokenizerResource("classpath:/spring-ai-onnx-model/KR-SBERT-V40K-klueNLI-augSTS/tokenizer.json");
        embeddingModel.setModelResource("classpath:/spring-ai-onnx-model/KR-SBERT-V40K-klueNLI-augSTS/model.onnx");
        embeddingModel.setTokenizerOptions(Map.of("padding", "true"));
        embeddingModel.setModelOutputName("token_embeddings");
        embeddingModel.afterPropertiesSet();

        var pgVectorStore = new PgVectorStore(jdbcTemplate, embeddingModel);

        return pgVectorStore;
    }

    @Bean
    ApplicationRunner runner(VectorStore pgVectorStore) {
        return args -> {
            log.info("Loading file(s) as Documents");
            var textReader = new TextReader(resource);
            textReader.setCharset(StandardCharsets.UTF_8);
            List<Document> documents = textReader.get();

            TextSplitter textSplitter = new TokenTextSplitter();
            List<Document> splitDocuments = textSplitter.apply(documents);
            pgVectorStore.add(splitDocuments);
        };
    }
}
