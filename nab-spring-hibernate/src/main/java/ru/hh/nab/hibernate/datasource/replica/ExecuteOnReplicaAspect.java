package ru.hh.nab.hibernate.datasource.replica;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.SessionFactory;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.PlatformTransactionManager;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_NOT_SUPPORTED;
import org.springframework.transaction.support.TransactionTemplate;
import ru.hh.nab.hibernate.transaction.DataSourceContext;

@Aspect
@Order(value = 0)
public class ExecuteOnReplicaAspect {

  private final PlatformTransactionManager transactionManager;
  private final SessionFactory sessionFactory;

  public ExecuteOnReplicaAspect(PlatformTransactionManager transactionManager, SessionFactory sessionFactory) {
    this.transactionManager = transactionManager;
    this.sessionFactory = sessionFactory;
  }

  @Around(value = "@annotation(replica)", argNames = "pjp,replica")
  public Object executeOnReplica(final ProceedingJoinPoint pjp, final ExecuteOnReplica replica) {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(PROPAGATION_NOT_SUPPORTED);
    transactionTemplate.setReadOnly(true);
    return DataSourceContext.onReplica(() -> transactionTemplate.execute(new ReplicaTransactionCallback(pjp, sessionFactory, replica)));
  }
}
