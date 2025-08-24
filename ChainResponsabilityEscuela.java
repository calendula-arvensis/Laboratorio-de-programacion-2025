
// Handler Interface

import java.util.Scanner;

interface SupportHandler {
    void handleRequest(Request solicitud);
    void setNextHandler(SupportHandler nextHandler);
}

// Concrete Handlers
class HandlerProfesor implements SupportHandler {
    private SupportHandler nextHandler;

    public void setNextHandler(SupportHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    public void handleRequest(Request solicitud) {
        //En este handler, el profesor, si la solicitud es de prioridad BASIC, 
        // la acepta o rechaza de manera aleatoria. Si no tiene esa prioridad, la pasa al siguiente
        if (solicitud.getPriority() == Priority.BASIC) {
            System.out.println("Un profesor se encargó de tu solicitud. ");
            int num = (int)(Math.random() * 10);
            if(num%10==0){
                System.out.println("El profesor aceptó la solicitud");
            }else{
                System.out.println("El profesor rechazó la solicitud");
            }
        } else if (nextHandler != null) {
            nextHandler.handleRequest(solicitud);
        }
    }
}

class HandlerCoordinadorAcademico implements SupportHandler {
    private SupportHandler nextHandler;

    public void setNextHandler(SupportHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    public void handleRequest(Request solicitud) {
        if (solicitud.getPriority() == Priority.INTERMEDIATE) {
            System.out.println("El coordinador academico se encargó de la consulta. ");
            int num = (int)(Math.random() * 10);
            if(num%10==0){
                System.out.println("El coordinador academico aceptó la solicitud");
            }else{
                System.out.println("El coordinador academico rechazó la solicitud");
            }
        } else if (nextHandler != null) {
            nextHandler.handleRequest(solicitud);
        }
    }
}

class HandlerSecretaria implements SupportHandler {
    public void handleRequest(Request request) {
        if (request.getPriority() == Priority.CRITICAL) {
            System.out.println("La secretaria se encargó de la consulta.");
            int num = (int)(Math.random() * 10);
            if(num%10==0){
                System.out.println("La secretaria aceptó la solicitud");
            }else{
                System.out.println("La secretaria rechazó la solicitud");
            }
        } else {
            System.out.println("La solicitud no pudo ser tomada");
        }
    }

    public void setNextHandler(SupportHandler nextHandler) {
        // No hay handler siguiente luego del de secretaria
    }
}

// Request Class
class Request {
    //La clase Request seria la clase de las solicitudes, solo tiene prioridad
    private Priority priority;

    public Request(Priority priority) {
        this.priority = priority;
    }

    public Priority getPriority() {
        return priority;
    }
}

// Priority Enum
enum Priority {
    BASIC, INTERMEDIATE, CRITICAL, INVALID
}

// Main Class
public class ChainResponsabilityEscuela {
    //ejecutar desde el run de abajo 
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int opcion;
        Request solicitud;
        SupportHandler profesor = new HandlerProfesor();
        SupportHandler coordinadorAcademico = new HandlerCoordinadorAcademico();
        SupportHandler secretaria = new HandlerSecretaria();

        profesor.setNextHandler(coordinadorAcademico);
        coordinadorAcademico.setNextHandler(secretaria);
        System.out.println("Ingrese el numero de la solicitud que desea ingresar: ");
        System.out.println("\n 1. Permiso para entregar una tarea tarde. "
                          +"\n 2. Permiso para realizar un examen en otra fecha. "
                          +"\n 3. Permiso para cambio de curso. "
                          +"\n 4. Permiso para certificado de alumno regular. "
                          +"\n 5. Permiso para aprobar una beca. ");
        opcion= sc.nextInt();
        if (opcion==1 || opcion==2){
            solicitud = new Request(Priority.BASIC);
        }else if(opcion==3 || opcion ==4){
            solicitud = new Request (Priority.INTERMEDIATE);
        }else if (opcion ==5){
            solicitud = new Request (Priority.CRITICAL);
        }else{
            solicitud= new Request(Priority.INVALID);
        }
        profesor.handleRequest(solicitud);
    }
}