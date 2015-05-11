
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;

import processing.core.*;

public class Servidor extends PApplet {
	
	private Socket socket; // variable socket para la comunicacion
	private ServerSocket serv; // variable serversocket para el servidor
	
	private int puerto = 6780; // puerto de entrada 
	private Redes redes; // objeto de la clase redes para separar la comunicacion
	
	private String estado = ""; // el estado en el cual esta regar 
	
	private boolean automatico = false; // estado automatico o manual
	
	
	
	/**
	 *  inicio de programa, creacion de redes.
	 */
	public void setup(){
		
		
		this.size(300,300); // tamaño del lienzo
		//conectarse();
		redes = new Redes(this,automatico); 
	}
	
	public void draw(){
		
		actualizarConfig();
		redes.enviarDatos();
		redes.riegoAuto();
		fondo();
	}
	
	
	/**
	 *  verificacion de la variable automatico, modo de riego.
	 */
	public void actualizarConfig(){
		
		if(automatico != redes.getAuto()){
			automatico = redes.getAuto();
		}
	}
	
	
	/**
	 *  describe el fondo (texto, variables colores, verificacion 
	 * de modo manual o automatico
	 */
	public void fondo(){
		
		background(255);
		
		if(automatico){
		estado = "automatico";	
		}else{
			estado ="manual";
		}
		textSize(20);
		fill(0);
		textAlign(CENTER);
		text("Estado: "+estado,width/2,height/2-30);
		text("Luz: "+redes.getCLuz(),width/2,height/2-10);
		text("Humedad: "+redes.getCHumedad(),width/2,height/2+10);
		text("Temperatura: "+redes.getCTemperatura(),width/2,height/2+30);
		text("Regando: "+redes.getRegando(),width/2,height/2+50);
	}
	
	public static void main(String[] arg){
		
		PApplet.main(new String[] { "Servidor" });
	}
	
	
}
