import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import processing.core.*;
import processing.data.XML;

public class Redes extends Thread {
	private PApplet app; // objeto de processing
	private Socket socket; // objeto de socket para la comunicacion
	private ServerSocket serv; // objeto de serversocket para servidor
	private int puerto = 6780; // puerto de comunicacion

	private boolean enLinea = false; // estado de conexion
	private DataInputStream input; // canales de envio 
	private DataOutputStream output; // canal de entrada
	private int temperatura = 20; // entero de temperatura entrante
	private int xTempe = temperatura; // variable que registra la temperatura anterior para revisar cambios.
	private int luz = 0; // entero de luz entrante
	private int xLuz = luz; // variable que registra el estado anterior de luz para revisar cambios
	private int humedad = 0; //variable de humedad entrante
	private int xHumedad = humedad; // variable que registra el estado anterior de humedad para revisar cambios
	private int tiempo = 0; // tiempo de espera para ejecutar accion
	
	private boolean automatico; // estado del automatico
	private int cLuz = 0; // variable luz de configuracion
	private int cHumedad = 0; // variable de humedad de configuracion
	private int cTemperatura = 0; // variable de temperatura de configuracion 
	private boolean regando = false;// estado de riego
	
	private XML datos; // objeto de processing xml
	
	private ConexArduino cArdu; // objeto de la conexion en arduino
	private File file; // archivo para el datos de xml
	
	
	
	/**
	 * 
	 * @params app: objeto de processing
	 * @params auto: boolean para compartir el estado del riego.
	 */
	public Redes(PApplet app,boolean auto){
		
		this.app = app;
		file = new File("xml/configuracion.xml");
		datos = app.loadXML(file.getPath());
		actualizarDatos(); 
		//automatico = auto;
		cArdu = new ConexArduino(app);
		tiempo = app.millis();
		start(); // inicio del hilo local
	}
	
	
	
	/**
	 *  actualiza los datos entrantes del android.
	 */
	public void actualizarDatos(){
		
		XML[] configuraciones = datos.getChildren("dato");
		app.println("cantidad de datos: "+configuraciones.length);
		if(configuraciones.length == 0){// verifica si esta vacio.
			app.println("estableciendo datos");
			
			XML nuevoDato = datos.addChild("dato");
	        nuevoDato.setInt("temperatura", 0);
	        nuevoDato.setInt("luz", 0);
	        nuevoDato.setInt("humedad", 0);
	        nuevoDato.setString("automatico", "true");
	        
	        app.saveXML(datos, file.getPath());
	        datos = app.loadXML(file.getPath());
	        app.println("datos iniciales creados");
		}else{
			cLuz = configuraciones[0].getInt("luz");
			cHumedad = configuraciones[0].getInt("humedad");
			cTemperatura = configuraciones[0].getInt("temperatura");
			if(configuraciones[0].getString("automatico").equals("true")){ // recibe el estado guardado antes de ser cerrado.
				automatico = true;
			}else{
				automatico = false;
			}
			
			
		}
	}
	public boolean getAuto(){
		return automatico;
	}
	public int getCLuz(){
		return cLuz;
	}
	public int getCHumedad(){
		return cHumedad;
	}
	public int getCTemperatura(){
		return cTemperatura;
	}
	
	
	/**
	 *  abre los canales para la comunicacion tcp
	 */
	public void abrirSockets(){
		
		try{
			input = new DataInputStream(socket.getInputStream());
			output = new DataOutputStream(socket.getOutputStream());
			enLinea = true;
			output.writeBoolean(automatico); // envia el estado al android
		}catch(IOException e){
			
		}
	}
	
	
	
	/**
	 *  se encarga de verificar si los sockets estan abiertos y la comunicacion
	 * en linea para enviar los datos al android
	 */
	public void enviarDatos(){
		
		if(socket != null && enLinea){
			if(xTempe != temperatura || xLuz != luz || xHumedad != humedad){ // si hay diferencia entre las variables y su estado anterior
				xTempe = temperatura;
				xLuz = luz;
				xHumedad = humedad;
				try{
					app.println("enviando nuevos datos");
					output.writeUTF("datos");
					output.writeInt(temperatura);
					output.writeInt(luz);
					output.writeInt(humedad);
					app.println("datos enviados");
				}catch(IOException e){
					
				}
			}
			if(app.millis() - tiempo > 500){ // maquina de estado, tiempo de espera agotado comunica con las variables registradas por arduino
				tiempo = app.millis();
				temperatura = cArdu.getTemp();
				luz = cArdu.getLuz();
				humedad = cArdu.getHum();
			}
		}
	}
	
	
	
	/**
	 * 
	 * @Exception InterruptedException
	 *  funcion del thread para mantener la recepcion de mensajes activa
	 */
	public void run(){
		
		conectarse();
		abrirSockets();
		while(true){
			recibirMensajes();
			try{
			sleep(100);
			}catch(InterruptedException e){
				
			}
		}
	}
	
	
	
	/**
	 *  recibe y analiza mensajes entrantes del android y ejecuta
	 * acciones a la arduino.
	 */
	public void recibirMensajes(){
		
		try{
		app.println("esperando mensaje");
		String msg = input.readUTF();
		app.println("msg recibido: "+msg);
		if(msg.equals("modo")){
			automatico = input.readBoolean();
			XML[] configuraciones = datos.getChildren("dato");
			if(automatico){
				configuraciones[0].setString("automatico", "true");
				regando = false;
				cArdu.abrirLlave(regando);
			}else{
				configuraciones[0].setString("automatico", "false");
			}
			app.saveXML(datos, file.getPath());
	        datos = app.loadXML(file.getPath());
			
			
		}
		if(msg.equals("nueva config")){
			cTemperatura = input.readInt();
			cHumedad = input.readInt();
			cLuz = input.readInt();
			
			app.println("nueva configuracion | temp: "+cTemperatura+" humedad: "+cHumedad+" luz: "+cLuz);
			XML[] configuraciones = datos.getChildren("dato");
			configuraciones[0].setInt("luz", cLuz);
			configuraciones[0].setInt("humedad", cHumedad);
			configuraciones[0].setInt("temperatura", cTemperatura);
			
			app.saveXML(datos, file.getPath());
	        datos = app.loadXML(file.getPath());
		}
		if(msg.equals("mandar config")){
			
			output.writeUTF("va config");
			output.writeInt(cLuz);
			output.writeInt(cHumedad);
			output.writeInt(cTemperatura);
			app.println("config enviada");
		}
		
		if(msg.equals("regar")){
			if(input.readUTF().equals("si")){
				regando = true;
				cArdu.abrirLlave(regando);
			}else{
				regando = false;
				cArdu.abrirLlave(regando);
			}
			
		}
		}catch(IOException e){
			app.println("conexion perdida: "+e);
			enLinea = false;
			reiniciarCliente();// cancela la conexion actual
		}
	}
	
	
	/**
	 *  verifica el estado del riego con el configurado por el usuario
	 */
	public void riegoAuto(){
		
		if(automatico){
			if((cArdu.getTemp() < cTemperatura || cArdu.getLuz() < cLuz) && cArdu.getHum() < cHumedad){
				regando = true;
				cArdu.abrirLlave(regando);
			}else{
				regando = false;
				cArdu.abrirLlave(regando);
			}
		}
	}
	
	
	/**
	 *  reinicia el cliente para poder esperar uno nuevo
	 */
	public void reiniciarCliente(){
		
		try{	
			app.println("reiniciando cliente");
			socket.close();
			serv.close();
			serv = null;
			socket = null;
			conectarse();
			abrirSockets();

		}catch(IOException e){
			
		}
	}
	
	
	/**
	 *  abre el socket y pone en escucha al serversocket
	 */
	public void conectarse(){
		
		try{
			app.println("esperando cliente");
			serv = new ServerSocket(puerto);
			socket = serv.accept();
			app.println("cliente aceptado / conectado...");
		}catch(IOException e){
			app.println("error en conexion: "+e);
		}
	}
	
	public boolean getConexion(){
		return enLinea;
	}
	public boolean getRegando(){
		return regando;
	}
}
