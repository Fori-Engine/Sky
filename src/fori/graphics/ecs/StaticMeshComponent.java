package fori.graphics.ecs;

import fori.graphics.Mesh;
import fori.graphics.StaticMeshBatch;

public record StaticMeshComponent(StaticMeshBatch staticMeshBatch, Mesh mesh) {
}
