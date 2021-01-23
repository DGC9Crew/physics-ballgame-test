package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.ui.Picture;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 * @author normenhansen
 */
public class Main extends SimpleApplication implements ActionListener {
    private BulletAppState bulletAppState;
    RigidBodyControl ball_phy;
    Vector3f launchDir = new Vector3f();
    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();
    Node floor_geo;
    
    private boolean left = false, right = false, up = false, down = false;
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
        app.setPauseOnLostFocus(false);
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
       // bulletAppState.getPhysicsSpace().setAccuracy(0.001f)
        SSAOFilter ssaoFilter = new SSAOFilter(12.94f, 43.92f, 0.33f, 0.61f);
        fpp.addFilter(ssaoFilter);
        setDisplayFps(false);
        setDisplayStatView(false);
        viewPort.addProcessor(fpp);
        Box floor = new Box(20,1,20);
        /*
                
        Geometry floor_geo = new Geometry("Floor", floor);
        
        
        floor_geo.setMaterial(assetManager.loadMaterial("Materials/stone.j3m"));
        floor_geo.setLocalTranslation(0, -0.1f, 0);
        this.rootNode.attachChild(floor_geo);
          */
         floor_geo = (Node) assetManager.loadModel("Scenes/newScene.j3o");
        CollisionShape sceneShape
                = CollisionShapeFactory.createMeshShape(floor_geo);
        rootNode.attachChild(floor_geo);
        RigidBodyControl floor_phy = new RigidBodyControl(sceneShape, 0.0f);
        floor_geo.addControl(floor_phy);
        bulletAppState.getPhysicsSpace().add(floor_phy);
        floor_geo.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        
        Sphere sphere = new Sphere(32, 32, 0.4f, true, false);
        sphere.setTextureMode(TextureMode.Projected);
        Geometry ball_geo = new Geometry("cannon ball", sphere);
        ball_geo.setMaterial(assetManager.loadMaterial("Materials/ball.j3m"));
        rootNode.attachChild(ball_geo);
        ball_geo.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        ball_geo.setLocalTranslation(0,10,0);
        
        ball_phy = new RigidBodyControl(1f);
       
        ball_geo.addControl(ball_phy);
        bulletAppState.getPhysicsSpace().add(ball_phy);
        flyCam.setEnabled(false);
        cam.setLocation(ball_phy.getPhysicsLocation().addLocal(new Vector3f(10,10,0)));
        
        initKeys();
        
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.5f));
        rootNode.addLight(al);
        
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White);
        sun.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(sun);
       
        
        /* Drop shadows */
        final int SHADOWMAP_SIZE = 1024;
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr.setLight(sun);
        viewPort.addProcessor(dlsr);

        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, SHADOWMAP_SIZE, 3);
        dlsf.setLight(sun);
        dlsf.setEnabled(true);
        dlsr.setShadowIntensity(0.3f);
        fpp.addFilter(dlsf);
        viewPort.addProcessor(fpp);
    }

    public void initKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");
        inputManager.addListener(this, "Jump");
    }
    public void onAction(String binding, boolean isPressed, float tpf) {
        if (binding.equals("Left")) {
            left= isPressed;
        } else if (binding.equals("Right")) {
            right = isPressed;
        } else if (binding.equals("Up")) {
            up = isPressed;
        } else if (binding.equals("Down")) {
            down = isPressed;
        } else if (binding.equals("Jump")) {
            if (isPressed) {
                //ball_phy.setLinearVelocity(ball_phy.getLinearVelocity().addLocal(new Vector3f(0,10,0)));
            }
        }
    }
    @Override
    public void simpleUpdate(float tpf) {
        boolean ableToMove = false;
        CollisionResults results = new CollisionResults();
        // 2. Aim the ray from cam loc to cam direction.
        Ray ray = new Ray(ball_phy.getPhysicsLocation(), Vector3f.UNIT_Y.negate());
        // 3. Collect intersections between Ray and Shootables in results list.
        // DO NOT check collision with the root node, or else ALL collisions will hit the
        // skybox! Always make a separate node for objects you want to collide with.
        floor_geo.collideWith(ray, results);
        // 4. Print the results
        System.out.println("----- Collisions? " + results.size() + "-----");
        for (int i = 0; i < results.size(); i++) {
            // For each hit, we know distance, impact point, name of geometry.
            float dist = results.getCollision(i).getDistance();
            Vector3f pt = results.getCollision(i).getContactPoint();
            String hit = results.getCollision(i).getGeometry().getName();
            System.out.println("* Collision #" + i);
            System.out.println("  You shot " + hit + " at " + pt + ", " + dist + " wu away.");
        }
        // 5. Use the results (we mark the hit object)
        if (results.size() > 0) {
            // The closest collision point is what was truly hit:
            CollisionResult closest = results.getClosestCollision();
            if(closest.getDistance() < 1) {
                ableToMove = true;
            } else {
                ableToMove = false;
            }
            
        } else {
            
            
        }
    
        
        
        
        
        
        
        
        
        camDir.set(cam.getDirection()).multLocal(.5f).setY(0);
        camLeft.set(cam.getLeft()).multLocal(.5f).setY(0);
        launchDir.set(0, 0, 0);
        
        
        
        
        
        boolean ifMoved = false;
        if (left) {
            launchDir.addLocal(camLeft);
            ifMoved = true;
        }
        if (right) {
            launchDir.addLocal(camLeft.negate());
            ifMoved = true;
        }
        if (up) {
            launchDir.addLocal(camDir);
            ifMoved = true;
        }
        if (down) {
            launchDir.addLocal(camDir.negate());
            ifMoved = true;
        }
        if(ifMoved && ableToMove) {
            Vector3f newvelocity = ball_phy.getLinearVelocity().addLocal(launchDir);
            
            if(newvelocity.x > 10) {
                newvelocity.x = 10; 
            }
            if (newvelocity.x < -10) {
                newvelocity.x = -10;
            }
            
            if (newvelocity.z > 10) {
                newvelocity.z = 10;
            }
            if (newvelocity.z < -10) {
                newvelocity.z = -10;
            }
            ball_phy.setLinearVelocity(newvelocity);
           
            
        }
        cam.setLocation(ball_phy.getPhysicsLocation().addLocal(new Vector3f(15, 15, 0)));
        cam.lookAt(ball_phy.getPhysicsLocation(), Vector3f.UNIT_Y);
        System.out.println(ball_phy.getLinearVelocity());
        
       
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
