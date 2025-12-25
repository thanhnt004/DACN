-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create product_embeddings table
CREATE TABLE product_embeddings (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL,
    content TEXT,
    embedding vector(768),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    embedding_version INTEGER NOT NULL DEFAULT 1
);

-- Create indexes
CREATE INDEX idx_product_embeddings_product_id ON product_embeddings(product_id);
CREATE INDEX idx_product_embeddings_embedding ON product_embeddings USING ivfflat (embedding vector_cosine_ops);

-- Create trigger to update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_product_embeddings_updated_at
    BEFORE UPDATE ON product_embeddings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
