# Module Installation Instructions

Instructions for installing optional modules or repositories.

## Training Pipeline Module
   git clone https://github.com/PerErikGronvik/training-pipeline modules/training-pipeline (not exist)

## Model Governance Module
   git clone https://github.com/PerErikGronvik/model-governance modules/model-governance (not exist)

## Docker Compose Services

**Start all services**:
   docker compose --profile all up -d

**Start individual services**:
   docker compose --profile training up -d
   docker compose --profile mlflow up -d
   docker compose --profile governance up -d
   docker compose --profile ollama up -d
   docker compose --profile metaflow-ui up -d

**Service ports**:
- Training Pipeline: http://localhost:8003 (Not implemented)
- Ollama: http://localhost:11434
- MLFlow: http://localhost:5001
- Governance API: http://localhost:8001 (Not implemented)
- Metaflow UI: http://localhost:8002

**Stop services**:
   docker compose down

   Note: To restart services, use `docker compose restart` or stop and start specific profiles.
