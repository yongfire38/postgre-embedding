package com.example.postgre_embedding.vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FaqController {

    private final ChatClient chatClient;
    private PgVectorStore pgVectorStore = null;

    @Value("classpath:/prompts/prompt-template.st")
    private Resource ragPromptTemplate;

    public FaqController(ChatClient.Builder chatClientBuilder, PgVectorStore pgVectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.pgVectorStore = pgVectorStore;
    }

    @GetMapping("/api/faq")
    public String getFaq(@RequestParam(value="message", defaultValue = "일본에서 유명한 빵에 대해 알려 줘") String message) {
        List<Document> similarDocuments = pgVectorStore.similaritySearch(SearchRequest.query(message).withTopK(2));
        List<String> contentList = similarDocuments.stream().map(Document::getContent).toList();

        PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
        Map<String, Object> promptParameters = new HashMap<>();

        promptParameters.put("input", message);
        promptParameters.put("documents", String.join("\n", contentList));
        Prompt prompt = promptTemplate.create(promptParameters);

        return chatClient.prompt(prompt).call().content();
    }
    
}
