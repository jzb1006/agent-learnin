CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS public.customer_knowledge_vectors (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    content text,
    metadata json,
    embedding vector(1536)
);

CREATE INDEX IF NOT EXISTS customer_knowledge_vectors_embedding_idx
    ON public.customer_knowledge_vectors
    USING hnsw (embedding vector_cosine_ops);
