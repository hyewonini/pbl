import os
from dotenv import load_dotenv
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_community.document_loaders import TextLoader
from langchain.schema.output_parser import StrOutputParser
from langchain.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI
from langchain_community.retrievers import BM25Retriever
from langchain.retrievers import  EnsembleRetriever
from langchain_community.vectorstores import FAISS
from langchain_openai import OpenAIEmbeddings
from langchain.embeddings import CacheBackedEmbeddings
from langchain.storage import LocalFileStore

os.environ['KMP_DUPLICATE_LIB_OK']='True'

import torch
torch.set_num_threads(1)

import time

# API 키 정보 로드
load_dotenv()

# 로컬 파일 저장소 설정
store = LocalFileStore("./cache/")

class RagEnsemble:
    def __init__(self):
        self.model = ChatOpenAI(model="gpt-4o", temperature=0, stream=True)  # Enable streaming
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=1024,
            chunk_overlap=100
        )
        self.embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
        self.policy_retriever = None
        self.guideline_retriever = None
        self.rag_chain = None

        self.cached_embedder = CacheBackedEmbeddings.from_bytes_store(
            underlying_embeddings=self.embeddings,
            document_embedding_cache=store,
            namespace=self.embeddings.model
        )

    def format_docs(self, docs):
        return '\n\n'.join([d.page_content for d in docs])

    def rerank(self, query, docs):
        query_embedding = self.embeddings.embed_query(query)  # 질문 임베딩 생성
        doc_texts = [doc.page_content for doc in docs]  # 문서 텍스트 추출

        doc_embeddings = [self.embeddings.embed_query(doc) for doc in doc_texts]  # 문서 임베딩 생성
        scores = [torch.cosine_similarity(torch.tensor(query_embedding),
                                          torch.tensor(doc_embedding), dim=0).item()
                  for doc_embedding in doc_embeddings]  # 코사인 유사도 계산
        sorted_docs = [doc for _, doc in sorted(zip(scores, docs), key=lambda x: x[0], reverse=True)]  # 유사도 기준 정렬
        return sorted_docs

    def set_policy_retriever(self, policy_file_path: str):
        loaders = TextLoader(file_path=policy_file_path).load()
        docs = self.text_splitter.split_documents(loaders)

        vectorstore_faiss = FAISS.from_documents(docs, self.embeddings)
        bm25_retriever = BM25Retriever.from_documents(docs)
        faiss_retriever = vectorstore_faiss.as_retriever(search_kwargs={'k': 5})

        self.policy_retriever = EnsembleRetriever(
            retrievers=[bm25_retriever, faiss_retriever],
            weights=[0.5, 0.5]
        )

    def set_guideline_retriever(self, guideline_file_path: str):
        loaders = TextLoader(file_path=guideline_file_path).load()
        docs = self.text_splitter.split_documents(loaders)

        vectorstore_faiss = FAISS.from_documents(docs, self.embeddings)
        bm25_retriever = BM25Retriever.from_documents(docs)
        faiss_retriever = vectorstore_faiss.as_retriever(search_kwargs={'k': 5})

        self.guideline_retriever = EnsembleRetriever(
            retrievers=[bm25_retriever, faiss_retriever],
            weights=[0.5, 0.5]
        )

    def create_rag_chain(self):
        template = """
        ### 역할 설명
        당신은 개인정보처리방침을 작성지침에 따라 평가하는 평가자입니다. 사용자가 입력한 개인정보처리방침을 분석하고, 주어진 평가지표를 기반으로 해당 방침이 제대로 작성되었는지 평가하세요. 평가 결과는 구체적인 근거와 함께 제시해야 합니다.

        ### 평가 절차
        1. 작성지침(guideline)의 작성 예시와 개인정보처리방침(policy)을 비교합니다.
        2. 작성지침에 맞지 않거나 누락된 부분이 있으면 구체적으로 지적하고, 관련 작성지침을 참조하여 개선해야 할 사항을 제안합니다.
        3. 각 평가지표에 대해 명확한 평가 결과를 제공하고, 평가 근거를 설명합니다.
        4. 평가 결과와 함께, 평가한 개인정보처리방침의 문장 또는 문단과 참조한 작성지침의 내용을 출력합니다.

        ### 평가 항목
        - 개인정보의 처리 목적
        - 처리하는 개인정보의 항목 및 보유기간
        - 개인정보의 파기 절차 및 방법
         

        ### 입력
        - 작성지침 (guideline):
        {guideline}

        - 개인정보처리방침 (policy):
        {policy}

        ### 질문
        사용자가 입력한 개인정보처리방침(policy)이 작성지침(guideline)에 따라 잘 작성되었는지 평가하고, 각 평가지표에 대한 평가 결과를 구체적인 근거와 함께 제시하세요. 평가 결과와 함께, 평가 대상이 되는 개인정보처리방침의 문장이나 문단, 참조한 작성지침의 내용을 출력하세요.

        ### 출력 형식 예시
        1. 평가지표 1: 개인정보처리자가 제공하는 서비스에 대한 개인정보 처리 목적을 작성 지침에 기재된대로 누락 없이 기재하고 있는지, 개인정보처리방침의 처리 목적과, 관련된 개인정보처리방침 작성 지침을 출력하세요.
        - 평가 결과: (작성된 개인정보처리방침의 평가)
        - 근거: (개인정보 처리 목적 섹션에 대한 평가 근거를 {guideline}에서 찾아 출력하세요)
        - 처리방침 문장: (개인정보 처리 목적 섹션을 {policy}에서 찾아 출력하세요.)

        2. 평가지표 2: 개인정보처리자가 처리하는 개인정보의 항목 및 보유기간이 기재된 내용을 출력하세요.
        - 평가 결과: (작성된 개인정보처리방침의 평가)
        - 근거: ({guideline}의 처리하는 개인정보 항목, 개인정보의 처리 목적, 개인정보의 처리 및 보유기간 섹션에서 평가 근거를 찾아 출력하세요)
        - 처리방침 문장: (개인정보의 처리 및 보유 기간 섹션을 {policy}에서 찾아 출력하세요.)

        3. 평가지표 3: 개인정보 파기에 관한 사항과 절차 및 방법을 기재된 대로 출력하세요.
        - 평가 결과: (작성된 개인정보처리방침의 평가)
        - 근거: ({guideline}에서 개인정보의 파기 절차 및 방법에 관한 사항을 찾아 출력하세요.)
        - 처리방침 문장: (개인정보의 파기 절차 및 방법에 관한 사항 섹션을 {policy}에서 찾아 출력하세요.)
      
        
         
        """
        
        self.prompt = ChatPromptTemplate.from_template(template)
        self.rag_chain = (
            self.prompt | self.model | StrOutputParser()
        )

    def ask(self, query: str, policy_text_path: str, guideline_text_path: str):
        if not self.rag_chain:
            return "평가할 체인을 먼저 설정하세요."

        start_time = time.time()

        # Guideline 문서 처리
        result_guideline = self.guideline_retriever.invoke(guideline_text_path)
        reranked_guideline_docs = self.rerank(query, result_guideline)
        formatted_guideline_docs = self.format_docs(reranked_guideline_docs)

        # Policy 문서 처리
        result_policy = self.policy_retriever.invoke(policy_text_path)
        reranked_policy_docs = self.rerank(query, result_policy)
        formatted_policy_docs = self.format_docs(reranked_policy_docs)

        # RAG 체인 실행
        response_generator = self.rag_chain.stream({
            "guideline": formatted_guideline_docs,
            "policy": formatted_policy_docs,
            "question": query
        })

        # Stream the response
        for response_part in response_generator:
            print(response_part, end='', flush=True)

        end_time = time.time()
        elapsed_time = end_time - start_time
        print(f"\n응답 시간: {elapsed_time:.2f}초")

    def clear(self):
        self.policy_retriever = None
        self.guideline_retriever = None
        self.rag_chain = None


def main():
    gpt_rag = RagEnsemble()
    
    # 파일 경로 입력 받기
    policy_file_path = input("개인정보처리방침 텍스트 파일의 경로를 입력하세요: ")
    guideline_file_path = input("개인정보처리방침 작성지침 텍스트 파일의 경로를 입력하세요: ")
    
    # 각각의 retriever 설정
    gpt_rag.set_policy_retriever(policy_file_path)
    gpt_rag.set_guideline_retriever(guideline_file_path)

    # RAG 체인 생성
    gpt_rag.create_rag_chain()

    while True:
        user_query = input("질문을 입력하세요 (종료하려면 'exit' 입력): ")
        if user_query.lower() == 'exit':
            break

        gpt_rag.ask(user_query, policy_file_path, guideline_file_path)


if __name__ == "__main__":
    main()
