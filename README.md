# ADC Project 2025/2026 - IndividualEvaluation

## Info About Student
Author: Pedro do Paço Pereira

Nº: 63118

Email: pp.pereira@campus.fct.unl.pt

---

## Project Details

```bash
Git Repository: https://github.com/pedropereira-fct/ADC-IndividualEvaluation-63118
Google Cloud Deployment URL: https://adc2526-indeval.ey.r.appspot.com/rest/<operation>
Local Deployment URL: http://localhost:8080/rest/<operation>
```

---

## Cloning the Project

### 1. Fork and clone the repository

Clone locally:

```bash
git clone git@github.com:pedropereira-fct/ADC-IndividualEvaluation-63118
cd ind-eval
```

### 2. Import into Eclipse

1. Open Eclipse and go to **File → Import → Maven → Existing Maven Projects**
2. Navigate to the folder where you cloned the project
3. Select it and click **Finish**
4. Eclipse will resolve dependencies automatically — check for any errors in the **Problems** tab

## Commands:

```bash
- mvn clean package
- mvn appengine:run
- mvn appengine:deploy -Dapp.deploy.projectId=adc2526-indeval -Dapp.deploy.version=1
```
