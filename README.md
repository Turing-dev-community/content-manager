# Content Manager Service

A Spring Boot service that helps create, manage, validate and moderate content. This service provides RESTful APIs for managing various types of content including users, blog posts, customer support requests, and support responses.

## How to Contribute

1. Check the list of features below to avoid duplicates
1. Mention the feature in the Slack channel
1. Create the issue:
   1. Create a new issue with the feature details from the feature list.
   1. **Use the following structure**: `Summary`, `Current Behavior`, `Expected Behavior`, `Additional Context` (see an 
      example [here](https://github.com/Turing-dev-community/content-manager/issues/42))
1. Implement the feature:
   1. Create a new branch for your feature named `feature/your-feature-name` or `fix/your-bug-fix`
   1. Implement the feature or bug fix
   1. **Update the feature list**
   1. Create a pull request to merge your changes back to main
   1. Inform repository owner for review

## Features

### List of Features

Please update for each new feature:

| date (YY-MM-DD) | contributor email | feature summary | feature description                                                                                                           | issue link                                                               |
|-----------------|-------------------|------------|-------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| 2025-11-12      |denis.h@turing.com | Add ednpoint to run validations against all blog posts | Endpoint `api/users/:id/validate-blogposts` that runs validations against all blog posts owned by the user and persists them. | [#52](https://github.com/Turing-dev-community/content-manager/issues/52) |
| 2025-11-13      |denis.h@turing.com | Validation Reports | Added new endpoint `/api/users/:id/validation-report` that provides a summary report of validation results for all content owned by a user. | [#53](https://github.com/Turing-dev-community/content-manager/issues/57) |
| 2025-11-14      |denis.h@turing.com | Introduce a Generic Content Model | More flexible content creation by providing a generic content model that allows custom content types and fields | [#61](https://github.com/Turing-dev-community/content-manager/issues/61) |
| 2025-11-14      | riddhi.s@turing.com    | Add pagination to GET /api/blogposts               | Add `page` and `size` query params to blog post list endpoint. Return paginated response with metadata.                       | [#55](https://github.com/Turing-dev-community/content-manager/issues/55) |
| 2025-11-17 | riddhi.s@turing.com | Add ProductOffer content type | New marketplace-ready content type with pricing, stock, delivery fields. Model + repository + schema. No API yet. | [#74](https://github.com/Turing-dev-community/content-manager/issues/74) |
| 2025-11-17      | pushpendra.s@turing.com | Add Comments to Blog Posts                                    | Enhance the BlogPost functionality to support a list of comments. Each blog post may contain multiple comments, and each comment should include.                     | [#75](https://github.com/Turing-dev-community/content-manager/issues/75) |

### Overview
- **Content Management**: Create, read, update, and delete various content types
- **Validation Pipeline**: Configurable validation system with support for length checks and custom validators
- **Database Support**: H2 for testing, MySQL for production
- **Profile-based Configuration**: Separate configurations for test and production environments


### How to Check Feature Details and Implementations

You can find the details for the currently available features by checking the web and service layer of the app:
Each domain(-entity) has its own package (e.g. [content](https://github.com/Turing-dev-community/content-manager/tree/main/src/main/java/com/dehold/contentmanager/content) that contains various content types, or [validation](https://github.com/Turing-dev-community/content-manager/tree/main/src/main/java/com/dehold/contentmanager/validation)
that is responsible for content validation). Each of these packages has different layers:

- `model`: db entities
- `repository`: db operations
- `service`: domain logic
- `web`: controllers etc

By checking the `web` and `service` layers, you can get a good idea of the details of each api.

**Some high level design decisions:**
- The layering should be the same across domains (mode, repository, service, web)
- jdbcTemplate with SQL is used to have control over the queries
- DTOs are used to map requests to the domain models

## Technology Stack

- **Framework**: Spring Boot 3.5.6
- **Database**: H2 (test), MySQL (production)
- **Java Version**: 24
- **Build Tool**: Maven
- **Testing**: JUnit 5, Spring Boot Test

## Getting Started

### Prerequisites

- Java 24 or higher
- Maven 3.6+
- MySQL (for production)

### Running the Application

#### Development Mode (H2 Database)
```bash
./mvnw spring-boot:run
```

#### Production Mode (MySQL Database)
```bash
./mvnw spring-boot:run --spring.profiles.active=prod
```

#### Running Tests
```bash
./mvnw test
```

## API Endpoints

### User Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/{id}` | Get user by ID |
| POST | `/api/users` | Create new user |
| PUT | `/api/users/{id}` | Update existing user |
| DELETE | `/api/users/{id}` | Delete user |
| GET | `/api/users/{id}/blogposts` | Retrieve blog posts owned by a specific user |

**User Model:**
```json
{
  "id": "uuid",
  "alias": "string",
  "email": "string",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### Blog Posts

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/blog-posts/{id}` | Get blog post by ID |
| POST | `/api/blog-posts` | Create new blog post |
| PUT | `/api/blog-posts/{id}` | Update existing blog post |
| DELETE | `/api/blog-posts/{id}` | Delete blog post |
| GET | `/api/blogposts?userId={userId}` | Filter blog posts by user (optional query parameter) |

> **Note:** The endpoint `/api/blogposts/user/{userId}` is now deprecated. Use `/api/blogposts?userId={userId}` instead.

**Blog Post Model:**
```json
{
  "id": "uuid",
  "title": "string",
  "content": "string",
  "createdAt": "timestamp",
  "updatedAt": "timestamp",
  "userId": "uuid"
}
```

### Customer Support Requests

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/customer-requests` | Get all customer requests |
| GET | `/api/customer-requests/{id}` | Get customer request by ID |
| POST | `/api/customer-requests` | Create new customer request |
| PUT | `/api/customer-requests/{id}` | Update existing customer request |
| DELETE | `/api/customer-requests/{id}` | Delete customer request |

**Customer Request Model:**
```json
{
  "id": "uuid",
  "text": "string",
  "supportResponse": "uuid",
  "customerId": "uuid",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### Support Responses

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/support-responses/{id}` | Get support response by ID |
| POST | `/api/support-responses` | Create new support response |
| PUT | `/api/support-responses/{id}` | Update existing support response |
| DELETE | `/api/support-responses/{id}` | Delete support response |

**Support Response Model:**
```json
{
  "id": "uuid",
  "text": "string",
  "supportRequest": "uuid",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### Validation

| Method | Endpoint | Description                                                                                                                                                                                                                                                            |
|--------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| POST   | `/api/validate/blogpost` | Validate a blog post                                                                                                                                                                                                                                                   |
| POST   | `/api/validate/validate-blogposts?userId={userId}` | Run validation on all blog posts for a specific user (This has an empty body as it validated blog posts that are stored to the db against validation pipelines that are stored in the db for that specific user). The response is listed under "Bulk Validation Response Example" |

**Validation Request Model:**
```json
{
  "titleMinLength": "integer",
  "titleMaxLength": "integer",
  "contentMinLength": "integer",
  "contentMaxLength": "integer",
  "blogPost": {
    "id": "uuid",
    "title": "string",
    "content": "string",
    "createdAt": "timestamp",
    "updatedAt": "timestamp",
    "userId": "uuid"
  }
}
```

**Validation Response Model:**
```json
{
  "contentType": "string",
  "validationResult": {
    "contentType": "string",
    "contentId": "uuid",
    "userId": "uuid",
    "valid": "boolean",
    "errors": [
      {
        "code": "string",
        "message": "string"
      }
    ]
  }
}
```

**Bulk Validation Response Example:**
```json
[
  {
    "contentType": "string",
    "validationResult": {
      "contentType": "string",
      "contentId": "uuid",
      "userId": "uuid",
      "valid": true,
      "errors": []
    }
  },
  {
    "contentType": "string",
    "validationResult": {
      "contentType": "string",
      "contentId": "uuid",
      "userId": "uuid",
      "valid": false,
      "errors": [
        {
          "code": "string",
          "message": "string"
        }
      ]
    }
  },
  {
    "contentType": "string",
    "validationResult": {
      "contentType": "string",
      "contentId": "uuid",
      "userId": "uuid",
      "valid": false,
      "errors": [
        {
          "code": "string",
          "message": "string"
        }
      ]
    }
  }
]
```

### Validation Pipelines

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/validation-pipelines/{id}` | Get validation pipeline by ID |
| GET | `/api/validation-pipelines?userId={userId}&contentType={contentType}` | Get validation pipelines by user ID and content type |
| POST | `/api/validation-pipelines` | Create new validation pipeline |
| PUT | `/api/validation-pipelines/{id}` | Update existing validation pipeline |
| DELETE | `/api/validation-pipelines/{id}` | Delete validation pipeline |

**Validation Pipeline Model:**
```json
{
  "id": "uuid",
  "userId": "uuid",
  "description": "string",
  "contentType": "string",
  "steps": [
    {
      "id": "uuid",
      "stepType": "LENGTH_VALIDATION",
      "fieldName": "string",
      "parameters": {
        "minLength": "string",
        "maxLength": "string"
      },
      "enabled": "boolean"
    }
  ],
  "createdAt": "timestamp"
}
```

**Create Pipeline Request Example:**
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "description": "Blog post validation pipeline",
  "contentType": "blogpost",
  "steps": [
    {
      "stepType": "LENGTH_VALIDATION",
      "fieldName": "title",
      "parameters": {
        "minLength": "5",
        "maxLength": "100"
      },
      "enabled": true
    },
    {
      "stepType": "LENGTH_VALIDATION",
      "fieldName": "content",
      "parameters": {
        "minLength": "10",
        "maxLength": "1000"
      },
      "enabled": true
    }
  ]
}
```

**Update Pipeline Request Example:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174001",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "description": "Updated blog post validation pipeline",
  "contentType": "blogpost",
  "steps": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174002",
      "stepType": "LENGTH_VALIDATION",
      "fieldName": "title",
      "parameters": {
        "minLength": "3",
        "maxLength": "150"
      },
      "enabled": true
    }
  ]
}
```

### Forbidden Words Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/forbidden-words` | Get all forbidden words (includes system defaults) |
| GET | `/api/forbidden-words/{id}` | Get forbidden words by ID |
| POST | `/api/forbidden-words` | Create new forbidden words |
| PUT | `/api/forbidden-words/{id}` | Update existing forbidden words |
| DELETE | `/api/forbidden-words/{id}` | Delete forbidden words |

**Forbidden Words Model:**
```json
{
  "id": "uuid",
  "userId": "uuid",
  "description": "string",
  "contentType": "string",
  "fieldName": "string",
  "words": ["string"],
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

**Create/Update Request Examples:**

*Create Request:*
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "description": "Inappropriate words for blog posts",
  "contentType": "blogpost",
  "fieldName": "content",
  "words": ["badword1", "inappropriate", "spam"]
}
```

*Update Request (PUT - all fields required):*
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174001",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "description": "Updated inappropriate words for blog posts",
  "contentType": "blogpost",
  "fieldName": "content",
  "words": ["badword1", "inappropriate", "spam", "newbadword"]
}
```

> **Note:** PUT requests require all fields to be present. Missing fields will result in a 400 Bad Request with details about which fields are missing. The path ID must match the payload ID.
