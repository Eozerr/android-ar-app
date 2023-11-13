package com.xperiencelabs.armenu

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.ar.core.Config
import com.google.ar.sceneform.math.Vector3
import com.xperiencelabs.armenu.ui.theme.ARMenuTheme
import com.xperiencelabs.armenu.ui.theme.Translucent
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.localRotation
import io.github.sceneview.ar.localScale
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ARMenuTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()){
                        val currentModel = remember {
                            mutableStateOf("leather_sofa")
                        }
                        val modelNode = remember{
                            mutableStateOf<ArModelNode?>(null)
                        }
                        val placeModelButton =remember{
                            mutableStateOf(false)
                        }
                        ARScreen(
                            model =currentModel.value,
                            modelNode = modelNode,
                            placeModelButton = placeModelButton
                        )
                        Menu(modifier = Modifier.align(Alignment.BottomCenter),
                            onClick = {
                                //Ar model tanımı burada yapılır.
                                currentModel.value=it
                            },
                            onUnanchor = {
                                //sabitlemeyi kaldırır ve placemodel butona geçilebilir hale getirir.
                                modelNode.value?.unanchor()
                                placeModelButton.value=true
                            }
                        )
                    }
                }
            }
        }
    }
}

fun ArModelNode.unanchor(){
    anchor?.detach()
    anchor=null
}

@Composable
fun Menu(modifier: Modifier,onClick:(String)->Unit, onUnanchor:()-> Unit) {
    var currentIndex by remember {
        mutableStateOf(0)
    }

    val itemsList = listOf(
        Furniture("sofa", R.drawable.leather_sofa),
        Furniture("chair", R.drawable.chair),
        Furniture("wall_plant", R.drawable.wall_plant),
        Furniture("pouf", R.drawable.pouf),
        Furniture("tekli", R.drawable.tekli),
        Furniture("whites", R.drawable.whites),
        Furniture("Sandalye", R.drawable.sandalye),
    )
    fun updateIndex(offset:Int){
        currentIndex = (currentIndex+offset + itemsList.size) % itemsList.size
        onClick(itemsList[currentIndex].name)
        // Sağa veya sola tıklandığında sabitlemeyi otomatik kaldırır.
        onUnanchor()
    }
    Row(modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        IconButton(onClick = {
            updateIndex(-1)
        }) {
            Icon(painter = painterResource(id = R.drawable.baseline_arrow_back_ios_24), contentDescription ="previous" )
        }

        CircularImage(imageId = itemsList[currentIndex].imageId )

        IconButton(onClick = {
            updateIndex(1)
        }) {
            Icon(painter = painterResource(id = R.drawable.baseline_arrow_forward_ios_24), contentDescription ="next")
        }
    }

}

@Composable
fun CircularImage(
    modifier: Modifier=Modifier,
    imageId: Int
) {
    Box(modifier = modifier
        .size(140.dp)
        .clip(CircleShape)
        .border(width = 3.dp, Translucent, CircleShape)
    ){
        Image(painter = painterResource(id = imageId), contentDescription = null, modifier = Modifier.size(140.dp), contentScale = ContentScale.FillBounds)
    }
}
@Composable
fun ARScreen(model: String, modelNode: MutableState<ArModelNode?>, placeModelButton: MutableState<Boolean>) {
    val nodes = remember {
        mutableListOf<ArNode>()
    }


    val rotationState = remember {
        mutableStateOf(0f)
    }
    val scaleState = remember {
        mutableStateOf(1f)
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset(0f, 0f))}

    Box(modifier = Modifier.fillMaxSize()
        .pointerInput(Unit){
            detectTransformGestures { _, pan, zoom, _ ->
                scale *= zoom
                offset = if (scale > 1f) {
                    Offset(
                        offset.x + pan.x * zoom,
                        offset.y + pan.y * zoom
                    )
                } else {
                    Offset(0f, 0f)
                }
            }
        }
    ) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            nodes = nodes,
            planeRenderer = true,
            onCreate = { arSceneView ->
                arSceneView.lightEstimationMode = Config.LightEstimationMode.DISABLED
                arSceneView.planeRenderer.isShadowReceiver = false
                modelNode.value = ArModelNode(arSceneView.engine, PlacementMode.INSTANT).apply {
                    loadModelGlbAsync(
                        glbFileLocation = "models/${model}.glb",
                        scaleToUnits = 0.8f
                    ) {

                    }
                    onAnchorChanged = {
                        placeModelButton.value = !isAnchored
                    }
                    onHitResult = { node, hitResult ->
                        placeModelButton.value = node.isTracking
                    }

                }
                nodes.add(modelNode.value!!)
            },
            onSessionCreate = {
                planeRenderer.isVisible = false
            }
        )

        // Döndürme Slider'ı
        Slider(
            value = rotationState.value,
            onValueChange = { newRotation ->
                rotationState.value = newRotation
                modelNode.value?.apply {
                    val rotation = Rotation(0f, newRotation * 360f, 0f)
                    localRotation = rotation
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )




        // Diğer kontrol elemanları
        if (placeModelButton.value) {
            Button(onClick = {
                modelNode.value?.anchor()
                placeModelButton.value=false

            }, modifier = Modifier.offset(y = 200.dp).align(Alignment.Center)) {
                Text(text = "Place It")
            }
        }
    }

    LaunchedEffect(key1 = model) {
        modelNode.value?.loadModelGlbAsync(
            glbFileLocation = "models/${model}.glb",
            scaleToUnits = 0.8f
        )
        Log.e("errorloading", "ERROR LOADING MODEL")
    }
}



data class Furniture(var name:String,var imageId:Int)