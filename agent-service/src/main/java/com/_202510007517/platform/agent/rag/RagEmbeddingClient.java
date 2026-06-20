package com._202510007517.platform.agent.rag;

import java.util.List;

public interface RagEmbeddingClient {
    List<Double> embed(String text);
}
