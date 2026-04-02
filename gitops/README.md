# GitOps manifests (business repo)

## What this repository uses by default

- `base/` + `overlays/prod/` are the **active** manifests consumed by Argo.
- `argocd/application.yaml` is optional backup; in this platform, CDK usually creates the Argo `Application` automatically.

## Prerequisites

- EKS cluster from `EksGitOpsStack` (or equivalent) and `kubectl` configured.
- Argo CD installed in namespace `argocd` (CDK stack installs it via Helm).

## Register this repository

`repoURL` 是 **Argo 拉清单的 Git**，不必与「推镜像」的 `jpos-template-card` 相同：可把本目录复制进业务仓并令 `repoURL` 指向它，或使用单独的 GitOps 仓库。

1. Edit `overlays/prod/kustomization.yaml`: `images[].newName` / `digest` should point to your ECR image.
2. (Optional) If you are not using CDK-managed Argo Application, edit `argocd/application.yaml`: `spec.source.repoURL`, `targetRevision`, `path`.

## APC runtime secret (example)

`base/deployment.yaml` already reads APC env vars from `Secret/apc-config` with `optional: true`.
Use the example file as template:

```bash
cp gitops/base/apc-config.secret.example.yaml /tmp/apc-config.secret.yaml
# edit values, then apply in your cluster
kubectl apply -f /tmp/apc-config.secret.yaml
```

Never commit real key aliases/regions if they are sensitive to your org policy.

## Apply the Application (optional)

Only needed when CDK does not create Argo Application (`createArgoApplication=false`).

```bash
kubectl apply -n argocd -f gitops/argocd/application.yaml
```

## Public URL (ALB)

After sync, the `demo-app` Ingress provisions an **internet-facing** ALB (AWS Load Balancer Controller, installed by CDK):

```bash
kubectl get ingress demo-app -n default -o wide
# ADDRESS column shows the ALB hostname; curl http://<hostname>/
```

Ensure `gitops/overlays/prod` image points at an image that exists in ECR (push via GitHub Actions first, or align tag/digest).

## Sync policy

`application.yaml` uses automated sync with `prune: true` and `selfHeal: false`. Tighten for production (e.g. manual sync, approvals, or ApplicationSet with promotion).
