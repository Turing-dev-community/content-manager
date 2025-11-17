CREATE TABLE IF NOT EXISTS "user" (
    id UUID PRIMARY KEY,
    alias VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
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

CREATE TABLE blog_post_history (
    id UUID PRIMARY KEY,
    blog_post_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    version_number INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Product Offer Table
CREATE TABLE IF NOT EXISTS product_offer (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    brand VARCHAR(255),
    category VARCHAR(255),
    original_price DECIMAL(12,2),
    offer_price DECIMAL(12,2) NOT NULL,
    discount_percentage INTEGER,
    stock_quantity INTEGER,
    delivery_time VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_offer_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS comments (
    id UUID PRIMARY KEY,
    blog_post_id UUID NOT NULL,
    user_id UUID NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_blog_post FOREIGN KEY (blog_post_id) REFERENCES blog_post (id) ON DELETE CASCADE
);