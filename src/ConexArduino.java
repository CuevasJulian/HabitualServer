import processing.serial.*;
import processing.core.*;

public class ConexArduino extends Thread {
	
	private Serial miArduino; // puerto serial de arduino
	private PApplet app; // obeto de processing
	private int temp; // entero recibido de temperatura
	private int luz; // entero recibido de luz
	private int hum; // entero recibido de humedad
	private boolean regar = false; // estado de riego
	
	/**
	 * @params app: objeto de processing
	 */
	public ConexArduino(PApplet app){
		
		for(int i = 0; i <Serial.list().length;i++){
			app.println("puertos en uso: "+Serial.list()[i]);
		}
		
		miArduino = new Serial(app,Serial.list()[(Serial.list().length)-1],9600);
		miArduino.bufferUntil('\n');
		start(); // inicio de hilo local 
	}
	
	/**
	 * hilo de thread mantiene conexion con la arduino
	 */
	public void run(){
		
		while(true){
			serialEvent(miArduino);
			try{
				sleep(500);
			}catch(InterruptedException e){
				
			}
		}
	}
	
	
	/**
	 *  evento de recepcion de mensajes de la arduino
	 * @exception NumerFormatException: describe un dato entero incapas de convertirse por formato
	 */
	public void serialEvent(Serial miArduino){
		
		if(miArduino.available() > 0){
			String msg = miArduino.readString();
			
			String[] datos = app.splitTokens(msg,"/");
			if(datos.length == 4){
				try{
					app.println("mensaje recibido Arduino: "+msg);
					temp = Integer.parseInt(datos[0]);
					luz =  Integer.parseInt(datos[1]);
					hum = Integer.parseInt(datos[2]);	
				}catch(NumberFormatException e){
					app.println("problemas: "+e);
				}
			}
		}
		
	}
	
	/**
	 * cambia el estado del riego, enviando mensaje a la arduino.
	 */
	public void abrirLlave(boolean regar){
		
		if(this.regar != regar){
			this.regar = regar;
			if(regar){
				miArduino.write(1);
			}else{
				miArduino.write(0);
			}
		}
	}
	
	public int getTemp(){
		return temp;
	}
	public int getLuz(){
		return luz;
	}
	public int getHum(){
		return hum;
	}

}
