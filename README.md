# postgre-embedding
Spring AI를 사용하여 RAG를 구현. Vector DB는 postgreSql을 사용

# 기본 사항
1. Onnx 형식으로 로컬에 익스포트해서 임베딩
2. 임베딩 후 적재할 때 Spring AI에서 제공되는 VectorStore를 사용
3. 벡터 데이터베이스로는 postgreSql 사용, supabase에서 호스팅 받음
4. Spring 버전은 6 이상, IDE로 Vscode 사용

# 환경 설정
- [해당 벡터 데이터베이스 가이드를 참조 가능하다](https://docs.spring.io/spring-ai/reference/1.0/api/vectordbs/pgvector.html)
- 가이드에서 제시한 쿼리는 다음과 같다

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
	id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
	content text,
	metadata json,
	embedding vector(1536) // 1536 is the default embedding dimension
);

CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops); 
```

- 익스텐션 3개가 잘 설치되어 있는지 확인하고 없으면 설치
- 차원 수는 원래 사용하던 한글 임베딩 모델의 차원수인 768로 설정함. (디폴트 값은 1536이기 때문)
- 그 외 나머지는 그대로 진행

# application.properties

- 접속정보는 supabase 페이지의 `Project setting – database settings – connection parameters` 에서 확인 가능

```
spring.ai.openai.api-key=노출되면 내 돈 나가서 큰일나는 키
spring.ai.openai.chat.options.model=gpt-4o

spring.application.name=postgre_embedding
spring.datasource.url= jdbc:postgresql://aws-0-ap-northeast-2.pooler.supabase.com:6543/postgres
spring.datasource.username=접속정보 페이지에 있는 유저명
spring.datasource.password=처음 생성했을 때 설정한 비번

spring.ai.vectorstore.pgvector.index-type=HNSW
spring.ai.vectorstore.pgvector.distance-type: COSINE_DISTANCE
spring.ai.vectorstore.pgvector.dimensions=768

spring.web.resources.add-mappings=true
```

# 모델 익스포트
- 모델은 해당 모델을 사용 : snunlp/KR-SBERT-V40K-klueNLI-augSTS
- [해당 가이드를 참조](https://docs.spring.io/spring-ai/reference/1.0/api/embeddings/onnx.html)
- 윈도우에서는 다음과 같이 진행
- 만들어진 파일은 `resources\spring-ai-onnx-model\KR-SBERT-V40K-klueNLI-augSTS` 경로에 넣음

```
python -m venv venv
.\venv\Scripts\activate
pip install --upgrade pip
pip install optimum onnx onnxruntime
(마지막에 오류나면 sentence-transformers도 추가)
pip install optimum onnx onnxruntime sentence-transformers

(모델 변경해서 현재 폴더에 내보내기)
optimum-cli export onnx -m snunlp/KR-SBERT-V40K-klueNLI-augSTS .
```

# 수행되는 기능
1. 준비된 텍스트 파일을 임베딩
2. 임베딩 값이 포함된 데이터를 DB에 적재
3. 프롬프트로 질의하였을 때 DB의 임베딩을 기반으로 유사 결과를 검색
4. 결과를 기반으로 답변을 작성해서 사용자에게 제시
