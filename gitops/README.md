# GitOps manifests (Argo CD)

## Prerequisites

- EKS cluster from `EksGitOpsStack` (or equivalent) and `kubectl` configured.
- Argo CD installed in namespace `argocd` (CDK stack installs it via Helm).

## Argo CD admin password

```bash
aws eks update-kubeconfig --name <ClusterName> --region <region>
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d && echo
```

Port-forward UI (chart uses `server.insecure` for local demo):

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:80
# Open http://localhost:8080  user: admin
```

## Register this repository

`repoURL` 是 **Argo 拉清单的 Git**，不必与「推镜像」的 `jpos-template-card` 相同：可把本目录复制进业务仓并令 `repoURL` 指向它，或使用单独的 GitOps 仓库。

1. Edit `argocd/application.yaml`: `spec.source.repoURL`, `targetRevision`, and `path` (e.g. `gitops/overlays/prod`).
2. Edit `overlays/prod/kustomization.yaml`: `images[].newName` / `newTag` → your ECR URI (image pushed by CI on **jpos-template-card**).

## Apply the Application

在**含有 `gitops/` 目录的仓库根目录**执行（或把 `-f` 换成文件绝对路径）：

```bash
kubectl apply -n argocd -f gitops/argocd/application.yaml
```

If the Git repo is private, configure a repository credential in Argo CD (SSH deploy key, GitHub App, or HTTPS token) before sync.

## Public URL (ALB)

After sync, the `demo-app` Ingress provisions an **internet-facing** ALB (AWS Load Balancer Controller, installed by CDK):

```bash
kubectl get ingress demo-app -n default -o wide
# ADDRESS column shows the ALB hostname; curl http://<hostname>/
```

Ensure `gitops/overlays/prod` image points at an image that exists in ECR (push via GitHub Actions first, or align tag/digest).

## Sync policy

`application.yaml` uses automated sync with `prune: true` and `selfHeal: false`. Tighten for production (e.g. manual sync, approvals, or ApplicationSet with promotion).
