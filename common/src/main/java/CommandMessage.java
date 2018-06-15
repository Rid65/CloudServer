

public class CommandMessage extends AbstractMessage  {

    private Commands command;
    private Object[] parameters;

    public CommandMessage(Commands command, Object ... parameters) {
        this.command = command;
        this.parameters = parameters;
    }

    public Commands getCommand() {
        return command;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public Object getParameter(int index) {
        return parameters[index];
    }
}
