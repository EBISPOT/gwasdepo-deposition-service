#!/bin/bash

kubectl delete deploy gwas-deposition-backend -n gwas
kubectl apply -f /home/tudor/Desktop/_deployment_plans_/gwas-deposition-backend-deployment.yaml
