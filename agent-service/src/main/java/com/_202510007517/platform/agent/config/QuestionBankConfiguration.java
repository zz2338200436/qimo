package com._202510007517.platform.agent.config;

import com._202510007517.platform.agent.questionbank.QuestionBankDocument;
import com._202510007517.platform.agent.questionbank.QuestionBankDocumentLoader;
import com._202510007517.platform.agent.questionbank.QuestionBankIndex;
import com._202510007517.platform.agent.questionbank.QuestionBankSummaryService;
import com._202510007517.platform.agent.questionbank.QuestionChunk;
import com._202510007517.platform.agent.questionbank.QuestionMarkdownParser;
import com._202510007517.platform.agent.questionbank.QuestionRagService;
import com._202510007517.platform.agent.rag.OllamaRagEmbeddingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(QuestionBankProperties.class)
public class QuestionBankConfiguration {
    private static final Logger log = LoggerFactory.getLogger(QuestionBankConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    QuestionBankDocumentLoader questionBankDocumentLoader() {
        return new QuestionBankDocumentLoader();
    }

    @Bean
    @ConditionalOnMissingBean
    QuestionMarkdownParser questionMarkdownParser() {
        return new QuestionMarkdownParser();
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.question-bank", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "questionBankEmbeddingClient")
    QuestionRagService.EmbeddingClient questionBankEmbeddingClient(QuestionBankProperties properties) {
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getEmbeddingBaseUrl())
                .build();
        OllamaRagEmbeddingClient delegate = new OllamaRagEmbeddingClient(restClient, properties.getEmbeddingModel());
        return delegate::embed;
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.question-bank", name = "enabled", havingValue = "true")
    List<QuestionChunk> questionBankQuestions(QuestionBankProperties properties,
                                              QuestionBankDocumentLoader loader,
                                              QuestionMarkdownParser parser) {
        List<QuestionBankDocument> documents = loader.load(properties.getDocumentPaths());
        List<QuestionChunk> questions = new ArrayList<>();
        for (QuestionBankDocument document : documents) {
            questions.addAll(parser.parse(document));
        }
        log.info("question bank loaded: documents={}, questions={}", documents.size(), questions.size());
        return List.copyOf(questions);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.question-bank", name = "enabled", havingValue = "true")
    QuestionBankIndex questionBankIndex(List<QuestionChunk> questionBankQuestions,
                                        QuestionRagService.EmbeddingClient embeddingClient) {
        List<QuestionBankIndex.IndexedQuestion> indexedQuestions = new ArrayList<>();
        for (QuestionChunk question : questionBankQuestions) {
            try {
                indexedQuestions.add(new QuestionBankIndex.IndexedQuestion(question, embeddingClient.embed(question.content())));
            } catch (RuntimeException ex) {
                log.info("failed to embed question chunk: questionId={}, reason={}",
                        question.questionId(), ex.getClass().getSimpleName());
                indexedQuestions.add(new QuestionBankIndex.IndexedQuestion(question, List.of()));
            }
        }
        QuestionBankIndex index = new QuestionBankIndex();
        index.replaceAll(indexedQuestions);
        return index;
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.question-bank", name = "enabled", havingValue = "true")
    QuestionRagService questionRagService(QuestionBankProperties properties,
                                          QuestionRagService.EmbeddingClient embeddingClient,
                                          QuestionBankIndex questionBankIndex) {
        return new QuestionRagService(properties, embeddingClient, questionBankIndex);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.question-bank", name = "enabled", havingValue = "true")
    QuestionBankSummaryService questionBankSummaryService(List<QuestionChunk> questionBankQuestions) {
        return new QuestionBankSummaryService(questionBankQuestions);
    }

    @Bean
    @ConditionalOnMissingBean(QuestionRagService.class)
    QuestionRagService disabledQuestionRagService() {
        QuestionBankProperties properties = new QuestionBankProperties();
        properties.setEnabled(false);
        return new QuestionRagService(properties, text -> List.of(), new QuestionBankIndex());
    }

    @Bean
    @ConditionalOnMissingBean(QuestionBankSummaryService.class)
    QuestionBankSummaryService disabledQuestionBankSummaryService() {
        return new QuestionBankSummaryService(List.of());
    }
}
