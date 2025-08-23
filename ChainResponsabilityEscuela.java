
// Handler Interface
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
    BASIC, INTERMEDIATE, CRITICAL
}

// Main Class
public class ChainResponsabilityEscuela {
    //Mejorar main luego
    public static void main(String[] args) {
        SupportHandler profesor = new HandlerProfesor();
        SupportHandler coordinadorAcademico = new HandlerCoordinadorAcademico();
        SupportHandler secretaria = new HandlerSecretaria();

        profesor.setNextHandler(coordinadorAcademico);
        coordinadorAcademico.setNextHandler(secretaria);

        Request request1 = new Request(Priority.BASIC);
        Request request2 = new Request(Priority.INTERMEDIATE);
        Request request3 = new Request(Priority.CRITICAL);

        profesor.handleRequest(request1);
        profesor.handleRequest(request2);
        profesor.handleRequest(request3);
    }
}