CREATE TABLE IF NOT EXISTS "user" (
    id UUID PRIMARY KEY,
    alias VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS blog_post (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    user_id UUID,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS support_response (
    id UUID PRIMARY KEY,
    user_id UUID,
    text TEXT NOT NULL,
    support_request UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS customer_request (
    id UUID PRIMARY KEY,
    user_id UUID,
    text TEXT NOT NULL,
    support_response UUID,
    customer_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS validation_result (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    content_id UUID NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    is_valid BOOLEAN NOT NULL,
    errors TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS forbidden_words (
    id UUID PRIMARY KEY,
    user_id UUID,
    description TEXT NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    field_name VARCHAR(255) NOT NULL,
    words TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE validation_pipeline (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    description VARCHAR(500),
    content_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE validation_step (
    id UUID PRIMARY KEY,
    pipeline_id UUID,
    step_type VARCHAR(50) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    parameters JSON,
    is_enabled BOOLEAN DEFAULT true
);

CREATE TABLE generic_content (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    fields JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    parent_id UUID,
    CONSTRAINT fk_generic_user FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE,
    CONSTRAINT fk_generic_parent FOREIGN KEY (parent_id) REFERENCES generic_content (id) ON DELETE SET NULL
);
