package com.comp460.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector3;
import com.comp460.components.CameraTargetComponent;
import com.comp460.components.TransformComponent;

/**
 * Created by matthewhammond on 1/15/17.
 */
public class CameraTrackingSystem extends IteratingSystem{

    private ComponentMapper<CameraTargetComponent> cameraTargetM;
    private ComponentMapper<TransformComponent> transformM;

    public CameraTrackingSystem() {
        super(Family.all(CameraTargetComponent.class, TransformComponent.class).get());
        cameraTargetM = ComponentMapper.getFor(CameraTargetComponent.class);
        transformM = ComponentMapper.getFor(TransformComponent.class);
    }
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        CameraTargetComponent cameraTargetComponent = cameraTargetM.get(entity);
        TransformComponent transformComponent = transformM.get(entity);

        cameraTargetComponent.camera.position.slerp(new Vector3(transformComponent.pos.x, transformComponent.pos.y, cameraTargetComponent.camera.position.z), 0.1f);
    }
}