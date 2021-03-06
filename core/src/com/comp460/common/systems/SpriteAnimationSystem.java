package com.comp460.common.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.comp460.common.components.AnimationComponent;
import com.comp460.common.components.TextureComponent;

/**
 * For each entity with both an animation and a texture advances the animation and updates the texture to the current
 * keyframe of the animation.
 */
public class SpriteAnimationSystem extends IteratingSystem {

    private static final Family animationFamily = Family.all(AnimationComponent.class, TextureComponent.class).get();

    private static final ComponentMapper<AnimationComponent> animationM = ComponentMapper.getFor(AnimationComponent.class);
    private static final ComponentMapper<TextureComponent> textureM = ComponentMapper.getFor(TextureComponent.class);

    public SpriteAnimationSystem(int priority) {
        super(animationFamily, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        AnimationComponent anim = animationM.get(entity);
        TextureComponent texture = textureM.get(entity);
        anim.timer +=deltaTime;
        texture.texture = anim.animation.getKeyFrame(anim.timer);
    }
}
